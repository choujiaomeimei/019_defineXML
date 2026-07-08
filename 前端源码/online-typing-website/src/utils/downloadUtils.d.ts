/**
 * 安全的文件下载工具类型声明
 */

export function safeDownload(blob: Blob, filename: string): void;

export function handleFileDownload(response: any, defaultFilename?: string): void;

export function downloadFromUrl(url: string, filename?: string): void; 