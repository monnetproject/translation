/**
 * *******************************************************************************
 * Copyright (c) 2011, Monnet Project All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met: *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. * Redistributions in binary
 * form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials provided
 * with the distribution. * Neither the name of the Monnet Project nor the names
 * of its contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE MONNET PROJECT BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * *******************************************************************************
 */
package eu.monnetproject.translation.chunker;

import java.util.regex.Pattern;

/**
 * Fairly good tokenizer all-purpose, all-weather model (TM)
 *
 * See http://maccaronic.wordpress.com/2012/11/20/a-simple-regular-expression-for-tokenization-of-most-natural-language/
 * 
 * @author John McCrae
 */
public class FairlyGoodTokenizer {

    private static final Pattern pattern1 = Pattern.compile("(\\.\\.\\.+|[\\p{Po}\\p{Ps}\\p{Pe}\\p{Pi}\\p{Pf}\u2013\u2014\u2015&&[^'\\.]]|(?<!(\\.|\\.\\p{L}))\\.(?=[\\p{Z}\\p{Pf}\\p{Pe}]|\\Z)|(?<!\\p{L})'(?!\\p{L}))");
    private static final Pattern pattern2 = Pattern.compile("\\p{C}|^\\p{Z}+|\\p{Z}+$");
    
    public static String[] split(String string) {
        final String s1 = pattern1.matcher(string).replaceAll(" $1 ");
        final String s2 = pattern2.matcher(s1).replaceAll("");
        return s2.split("\\p{Z}+");
    }
//    private static void replaceAll(StringBuilder sb, String match, String replace) {
//        final Pattern pattern = Pattern.compile(match);
//        Matcher matcher = pattern.matcher(sb);
//        int start = 0;
//        while (matcher.find(start)) {
//            final String replace2 = matcher.groupCount() > 0
//                    ? matcher.groupCount() > 1
//                    ? //  matcher.groupCount() > 2 ?
//                    //  replace.replaceAll("\\\\1", matcher.group(1)).replaceAll("\\\\2", matcher.group(2)).replaceAll("\\\\3", matcher.group(3)) :
//                    replace.replace("\\1", matcher.group(1)).replace("\\2", matcher.group(2))
//                    : replace.replace("\\1", matcher.group(1))
//                    : replace;
//            sb.replace(matcher.start(), matcher.end(), replace2);
//            start = matcher.start() + replace.length();
//            if (start >= sb.length()) {
//                break;
//            } else {
//                matcher = pattern.matcher(sb);
//            }
//        }
//    }
//
//    public static String[] split(String s) {
//        final StringBuilder sb = new StringBuilder(s);
//        // Generic rules
//        replaceAll(sb, "\\.\\.\\.", " ... ");
//        replaceAll(sb, "([\\p{Po}&&[^'\\.]])", " \\1 ");
//        replaceAll(sb, "([^\\.])\\.([\\]\\)\\}\\>\"'\\s])", "\\1 . \\2 ");
//        replaceAll(sb, "([^\\.])\\.$", "\\1 .");
//        replaceAll(sb, "([\\]\\[\\(\\)\\{\\}\\<\\>])", " \\1 ");
//        replaceAll(sb, "--", " -- ");
//        replaceAll(sb, "([^'])' ", "\\1 ' ");
//
//        // Extra unicode junk
//        replaceAll(sb, "\\s([\\p{Ps}\\p{Pi}])(\\S)", " \\1 \\2");
//        replaceAll(sb, "^([\\p{Ps}\\p{Pi}])(\\S)", "\\1 \\2");
//        replaceAll(sb, "\\s([\\p{Ps}\\p{Pi}])$", " \\1");
//        replaceAll(sb, "(\\S)([\\p{Pe}\\p{Pf}\u201f])\\s", "\\1 \\2 ");
//        replaceAll(sb, "^([\\p{Pe}\\p{Pf}\u201f])\\s", "\\1 ");
//        replaceAll(sb, "(\\S)([\\p{Pe}\\p{Pf}\u201f])$", "\\1 \\2");
//        replaceAll(sb, "\\p{C}", ""); // remove "control" characters
//
//        // Clean up extra
//        replaceAll(sb, "^ +", "");
//        replaceAll(sb, " +$", "");
//
//        // Format into a token list \\p{Z} is the Unicode generalization of \\s
//        return sb.toString().split("\\p{Z}+");
//    }
}
