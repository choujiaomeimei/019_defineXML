package com.stat.common.util;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 字段列表工具类
 * 处理字段选项的分隔符逻辑
 */
public class FieldListUtil {
    
    /**
     * 默认分隔符
     */
    public static final String DEFAULT_SEPARATOR = "#,";
    
    /**
     * 根据分隔符分割字段列表
     * 
     * @param fieldList 字段列表字符串
     * @param separator 分隔符，如果为空则使用默认分隔符
     * @return 分割后的字符串数组
     */
    public static String[] splitFieldList(String fieldList, String separator) {
        return splitFieldList(fieldList, separator, -1);
    }
    
    /**
     * 根据分隔符分割字段列表
     * 
     * @param fieldList 字段列表字符串
     * @param separator 分隔符，如果为空则使用默认分隔符
     * @param limit 分割限制参数，传递给split方法
     * @return 分割后的字符串数组
     */
    public static String[] splitFieldList(String fieldList, String separator, int limit) {
        if (StringUtils.isBlank(fieldList)) {
            return new String[0];
        }
        
        String actualSeparator = StringUtils.isBlank(separator) ? DEFAULT_SEPARATOR : separator;
        return fieldList.split(actualSeparator, limit);
    }
    
    /**
     * 根据分隔符分割字段列表，返回过滤后的List
     * 
     * @param fieldList 字段列表字符串
     * @param separator 分隔符，如果为空则使用默认分隔符
     * @return 分割后的字符串列表（过滤空值）
     */
    public static List<String> splitFieldListToList(String fieldList, String separator) {
        String[] array = splitFieldList(fieldList, separator);
        return Arrays.stream(array)
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .collect(Collectors.toList());
    }
    
    /**
     * 使用指定分隔符连接字段列表
     * 
     * @param fieldList 字段列表
     * @param separator 分隔符，如果为空则使用默认分隔符
     * @return 连接后的字符串
     */
    public static String joinFieldList(List<String> fieldList, String separator) {
        if (fieldList == null || fieldList.isEmpty()) {
            return "";
        }
        
        String actualSeparator = StringUtils.isBlank(separator) ? DEFAULT_SEPARATOR : separator;
        return String.join(actualSeparator, fieldList);
    }
    
    /**
     * 获取有效的分隔符
     * 
     * @param separator 输入的分隔符
     * @return 有效的分隔符
     */
    public static String getValidSeparator(String separator) {
        return StringUtils.isBlank(separator) ? DEFAULT_SEPARATOR : separator;
    }
}