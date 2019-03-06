package com.mcloyal.serialport.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * List数据集合分割工具类
 */
public class ListUtils {

    /**
     * 分割List
     *
     * @param list     待分割的list
     * @param size 每段list的大小
     * @return List<<List<T>>
     */
    public static <T> List<List<T>> splitList(List<T> list, int size) {
        int listSize = list.size();                                                           //list的大小
        int page = (listSize + (size - 1)) / size;                      //页数
        List<List<T>> listArray = new ArrayList<List<T>>();              //创建list数组 ,用来保存分割后的list
        for (int i = 0; i < page; i++) {                                                         //按照数组大小遍历
            List<T> subList = new ArrayList<T>();                               //数组每一位放入一个分割后的list
            for (int j = 0; j < listSize; j++) {                                                 //遍历待分割的list
                int pageIndex = ((j + 1) + (size - 1)) / size;   //当前记录的页码(第几页)
                if (pageIndex == (i + 1)) {                                               //当前记录的页码等于要放入的页码时
                    subList.add(list.get(j));                                               //放入list中的元素到分割后的list(subList)
                }
                if ((j + 1) == ((j + 1) * size)) {                               //当放满一页时退出当前循环
                    break;
                }
            }
            listArray.add(subList);                                                         //将分割后的list放入对应的数组的位中
        }
        return listArray;
    }


    public static void append(ArrayList<Byte> list, byte[] bb) {
        for (int i = 0; i < bb.length; i++) {
            list.add(bb[i]);
        }
    }


    public static void main(String[] s) {
        List<String> strlist = new ArrayList<String>();
        for (int i = 0; i < 20; i++) {
            strlist.add("aa" + (i + 1));
        }
        List<List<String>> list = splitList(strlist, 6);

        int index = 1;
        for (List<String> strlist2 : list) {
            System.out.println(index++);
            System.out.println("----------------------------------");
            for (String str : strlist2) {
                System.out.print(str + "\t");
            }
//            System.out.println("END");
            System.out.println();
        }
    }
}
