import moment from 'moment'

//计算正确率
export function computeCorrect(params) {
  return Math.round(
    (params.correctCount / (params.correctCount * 1 + params.errorCount * 1)) *
      100
  )
}
//计算分数
export function computeGrade(params) {
  let correct = computeCorrect(params)
  console.log('正确率', correct)
  if (params.speed < 30) {
    return 50
  } else if (params.speed == 30) {
    return 60
  } else if (params.speed > 30) {
    return 70
  } else if (params.speed >= 40) {
    return 80
  } else if (params.speed >= 50) {
    return 90
  } else if (params.speed >= 60) {
    return 100
  }
}

//返回当前时间
export function getDateTime() {
  // const currentDate = new Date()
  // const year = currentDate.getFullYear() // 获取年份
  // const month = currentDate.getMonth() + 1 // 获取月份（注意需要加1，因为月份从0开始）
  // const date = currentDate.getDate() // 获取日期
  // const hours = currentDate.getHours() // 获取小时
  // const minutes = currentDate.getMinutes() // 获取分钟
  // const seconds = currentDate.getSeconds() // 获取秒钟
  // const dateTime =
  //   year +
  //   '-' +
  //   month +
  //   '-' +
  //   date +
  //   ' ' +
  //   hours +
  //   ':' +
  //   minutes +
  //   ':' +
  //   seconds
  return moment().format('YYYY-MM-DD:HH:mm:ss')
}

export function isMobile() {
  //判断是否为移动端设备
  return /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(
    navigator.userAgent
  )
}
export function reloadFn() {
  /** 切换横竖屏时重载页面
   * @rule1 横屏切竖屏
   * @rule2 竖屏切横屏
   */
  window.addEventListener('resize', () => {
    let screenWidth = window.innerWidth || document.documentElement.clientWidth
    let screenHeight =
      window.innerHeight || document.documentElement.clientHeight
    const rule1 = screenWidth < screenHeight //手机竖屏
    const rule2 = screenWidth > screenHeight //手机横屏
    const isMobileX = isMobile() //判断是否为移动端设备
    if (rule1 && isMobileX) {
      //手机竖屏
      // window.location.reload() //重新刷新浏览器，根据项目需求，是否需要刷新整个页面
      console.log('手机竖屏的宽度', screenWidth)
      console.log('手机竖屏的高度', screenHeight)
    } else {
      //手机横屏
      // window.location.reload() //重新刷新浏览器，根据项目需求，是否需要刷新整个页面
      console.log('手机横屏的宽度', screenWidth)
      console.log('手机横屏的高度', screenHeight)
    }
  })
}
reloadFn()
