/**
 * Copyright © 2019, Glodon Digital Supplier BU.
 * <p>
 * All Rights Reserved.
 */

package com.gyoomi.jdk13;

/**
 * The description of class
 *
 * @author Leon
 * @date 2019-09-25 10:30
 */
public class TextBlockDemo {

    public static void main(String[] args) {
        String sql = " SELECT "
                   + "     t.* "
                   + " FROM user t";
        String html = "<html>"
                    + "    <head></head>"
                    + "    <body>"
                    + "        <font color='red'>测试字符</font>"
                    + "    </body>"
                    + "</html>";

        // JDK13的写法
        String sql13 = """
                        SELECT
                           t.*
                        FROM user t
                       """;

        String html13 = """
                           <html>
                                <head></head>
                                <body>
                                    <font color='red'>测试字符</font>
                                </body>
                           </html>
                        """;
    }
}
