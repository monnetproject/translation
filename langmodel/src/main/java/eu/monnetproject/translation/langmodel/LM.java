/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.monnetproject.translation.langmodel;

import eu.monnetproject.lang.Language;
import eu.monnetproject.translation.LanguageModel;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;

/**
 *
 * @author jmccrae
 */
public class LM {
   public static void main(String[] args) throws Exception {
       if(args.length != 1) {
           throw new IllegalArgumentException("Please specify language");
       }
       final LMFactory lmFactory = new LMFactory();
       final LanguageModel model = lmFactory.getModel(Language.get(args[0]));
       final BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
       String line;
       while((line = in.readLine()) != null) {
           System.out.println(model.score(Arrays.asList(line.split(" "))));
       }
   } 
}
