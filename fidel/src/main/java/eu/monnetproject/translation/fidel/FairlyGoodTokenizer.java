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
package eu.monnetproject.translation.fidel;

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
}
