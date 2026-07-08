/**
 * 安全的文件下载工具
 * 解决blob URL的HTTPS警告问题
 */

/**
 * 创建安全的下载链接
 * @param {Blob} blob - 文件数据
 * @param {string} filename - 文件名
 */
export function safeDownload(blob, filename) {
  try {
    // 首先尝试使用 msSaveBlob (IE)
    if (window.navigator && window.navigator.msSaveBlob) {
      window.navigator.msSaveBlob(blob, filename);
      return;
    }
    
    // 创建blob URL
    const url = window.URL.createObjectURL(blob);
    
    // 创建隐藏的下载链接
    const link = document.createElement('a');
    link.style.display = 'none';
    link.href = url;
    link.download = filename;
    
    // 添加到DOM并触发下载
    document.body.appendChild(link);
    link.click();
    
    // 清理
    setTimeout(() => {
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    }, 100);
    
  } catch (error) {
    console.error('下载失败:', error);
    // 备用方案：使用传统的window.open
    const reader = new FileReader();
    reader.onload = function(e) {
      const link = document.createElement('a');
      link.href = e.target.result;
      link.download = filename;
      link.click();
    };
    reader.readAsDataURL(blob);
  }
}

/**
 * 处理axios响应的文件下载
 * @param {Object} response - axios响应对象
 * @param {string} defaultFilename - 默认文件名
 */
export function handleFileDownload(response, defaultFilename = 'download') {
  try {
    // 从响应头获取文件名
    const contentDisposition = response.headers['content-disposition'];
    let filename = defaultFilename;
    
    if (contentDisposition) {
      const filenameMatch = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/);
      if (filenameMatch && filenameMatch[1]) {
        filename = decodeURIComponent(filenameMatch[1].replace(/['"]/g, ''));
      }
    }
    
    // 创建blob
    const blob = new Blob([response.data], { 
      type: response.headers['content-type'] || 'application/octet-stream' 
    });
    
    // 使用安全下载
    safeDownload(blob, filename);
    
  } catch (error) {
    console.error('处理文件下载失败:', error);
    throw error;
  }
}

/**
 * 处理URL直接下载
 * @param {string} url - 下载URL
 * @param {string} filename - 文件名
 */
export function downloadFromUrl(url, filename) {
  try {
    const link = document.createElement('a');
    link.style.display = 'none';
    link.href = url;
    if (filename) {
      link.download = filename;
    }
    
    document.body.appendChild(link);
    link.click();
    
    setTimeout(() => {
      document.body.removeChild(link);
    }, 100);
    
  } catch (error) {
    console.error('URL下载失败:', error);
    // 备用方案：打开新窗口
    window.open(url, '_blank');
  }
} 