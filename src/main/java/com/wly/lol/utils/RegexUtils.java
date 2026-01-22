package com.wly.lol.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexUtils {
    /*
    *通用正则提取器
        //编辑正则表达式
        // 将传入的字符串规则 (patternStr) 编译成 Java 能理解的 Pattern 对象
        // 然后创建一个匹配器 (Matcher)，把它绑定到你要搜索的内容 (source) 上
        // 第二步：查找并返回
        // matcher.find(): 尝试在 source 中寻找符合规则的“下一处”匹配
        // ?: 三元运算符。
        // 如果找到了 (true) -> 返回 matcher.group() (匹配到的具体内容)
        // 如果没找到 (false) -> 返回 null
    *
    */
    public static String extract(String source , String regex){
        if(source==null||regex==null) return null;
        Matcher matcher = Pattern.compile(regex).matcher(source);
    if(matcher.find()){
        return matcher.groupCount()>0?matcher.group(1):matcher.group();
    }
    return null;
    }
}
