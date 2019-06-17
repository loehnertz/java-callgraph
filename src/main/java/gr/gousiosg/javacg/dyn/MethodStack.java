/*
 * Copyright (c) 2011 - Georgios Gousios <gousiosg@gmail.com>
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package gr.gousiosg.javacg.dyn;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Stack;

public class MethodStack {
    private static Stack<String> stack = new Stack<>();
    static FileWriter fw;
    static StringBuffer sb;
    static long threadid = -1L;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    System.out.println("Recording finished!");
                    fw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        File log = new File("/tmp/calltrace.txt");

        try {
            fw = new FileWriter(log);
        } catch (Exception e) {
            e.printStackTrace();
        }

        sb = new StringBuffer();
    }

    public static void push(String callname) throws IOException {
        if (threadid == -1) threadid = Thread.currentThread().getId();
        if (Thread.currentThread().getId() != threadid) return;

        if (!stack.isEmpty()) {
            sb.setLength(0);
            sb.append(stack.peek()).append("\t").append(callname);
            sb.append("\n");
            fw.write(sb.toString());
        }

        stack.push(callname);
    }

    public static void pop() {
        if (threadid == -1) threadid = Thread.currentThread().getId();
        if (Thread.currentThread().getId() != threadid) return;

        stack.pop();
    }
}
