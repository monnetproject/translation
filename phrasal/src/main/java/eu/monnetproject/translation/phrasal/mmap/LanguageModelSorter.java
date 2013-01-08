/**********************************************************************************
 * Copyright (c) 2011, Monnet Project
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Monnet Project nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE MONNET PROJECT BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *********************************************************************************/
package eu.monnetproject.translation.phrasal.mmap;

import eu.monnetproject.translation.monitor.Messages;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 *
 * @author John McCrae
 */
public class LanguageModelSorter {


    public void sortModel(File languageModel, File outModel) throws IOException {
        final BufferedReader in = new BufferedReader(languageModel.getName().endsWith(".gz") 
                ? new InputStreamReader(new GZIPInputStream(new FileInputStream(languageModel)))
                : new FileReader(languageModel));
        final PrintWriter out = new PrintWriter(outModel);

        String s;
        while ((s = in.readLine()) != null) {
            out.println(s);
            if (s.matches("\\\\\\d+-grams.*")) {
                Messages.info(s);
                Messages.info("Reading...");

                List<String[]> lines = new ArrayList<String[]>();

                while (!(s = in.readLine()).matches("\\s*")) {
                    String[] parts = s.split("\t");
                    if (parts.length < 2) {
                        throw new IllegalArgumentException("Bad line: " + s);
                    }
                    lines.add(parts);
                }

                Messages.info("Sorting...");
                Collections.sort(lines, new Comparator<String[]>() {

                    @Override
                    public int compare(String[] o1, String[] o2) {
                        return (o1[1]).compareTo(o2[1]);
                    }
                });

                for (String[] strs : lines) {
                    for (int i = 0; i < strs.length; i++) {
                        out.print(strs[i]);
                        if(i + 1 != strs.length)
                            out.print("\t");
                    }
                    out.println();
                }
                out.println();
            }
        }
        out.flush();
        out.close();
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage:\n\tLanguageModelSorter languageModelIn languageModelOut");
            System.exit(-1);
        }
        new LanguageModelSorter().sortModel(new File(args[0]), new File(args[1]));
    }
}
