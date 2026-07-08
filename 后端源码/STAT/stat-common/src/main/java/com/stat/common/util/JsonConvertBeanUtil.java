package com.stat.common.util;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.beanutils.BeanUtils;

import java.util.List;
import java.util.Map;

/**
* @Program: json转bean
* @Description: 
* @Author: YuJQ
**/

public class JsonConvertBeanUtil {
	
  public static Object copyProperties(Object obj,Map map){
      try {
          BeanUtils.copyProperties(obj, map);
      } catch (Exception e) {

      }
      return obj;
  }
  //bean 转json
    public static String bean2json(Object object) {
        String jsonObject = JSONObject.toJSONString(object);
        return jsonObject;
    }
    //json 转 bean
    public static Object json2Object(String json, Class beanClz) {
        return JSONObject.parseObject(json, beanClz);
    }



    public static  List parseArray(String json, Class  clazz) {
        System.out.println(JSONArray.parseArray(json, clazz));
        return JSONArray.parseArray(json, clazz);
    }


}
