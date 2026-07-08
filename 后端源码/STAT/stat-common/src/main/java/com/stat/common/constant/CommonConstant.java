package com.stat.common.constant;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * @description:常量
 * @author: yujq
 **/
public interface CommonConstant {
    /**
     * common_token
     */
    String ACCESS_SECRET = "accesstoken";

    String ACCESS_TOKEN = "accesstoken";

    String USER_INFO = "user_info";

    String SESSION_KEY = "SESSION_KEY_IMAGE_CODE";

    String USER_LOGIN = "/user/login";

    String IMAGE_CODE = "imageCode";

    /**
     * 默认每页显示条数
     */
    Integer PAGE_SIZE = 20;

    Integer INITIALIZATION_NUMBER = 0;

    //使用
    Integer DELETEFLAG_0 =0;
    //禁用
    Integer DELETEFLAG_1 =1;


    //有下级
    Integer IS_DEEP_0 =0;
    //无下级
    Integer IS_DEEP__1 =1;


    //未推送
    Integer ISSEND_0 =0;
    //推送
    Integer ISSEND_1 =1;

    Integer ACCOUNT_TYPE_1 =1;
    /**
     * 定时器类
     */
    Map<Integer, ScheduledFuture> SCHEDULED_FUTURE = new ConcurrentHashMap<>();


    /**
     * redis devices
     */
    String DEVICES = "devices";
    /**
     * redis builder
     */
    String BUILDER = "builder";

    /**
     * redis alarms
     */
    String ALARMS = "alarms";
    /**
     * redis inform
     */
    String INFORM = "inform";
    /**
     * redis InformHistory
     */
    String INFORM_HISTORY = "inform_history";

    Integer MODULECODE_100000 = 100000;
    Integer MODULECODE_200000 = 200000;
    Integer MODULECODE_300000 = 300000;

    Integer MSGTYPE = 1;
    Integer MSGCODE = 200000;

    String CLIENTNEWCONTACTOR = "相关联系人 ";
}
