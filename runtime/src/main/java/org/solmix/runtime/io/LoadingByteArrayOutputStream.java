/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.solmix.runtime.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Subclass of ByteArrayOutputStream that allows creation of a
 * ByteArrayInputStream directly without creating a copy of the byte[].
 * 
 * Also, on "toByteArray()" it truncates it's buffer to the current size
 * and returns the new buffer directly.  Multiple calls to toByteArray() 
 * will return the exact same byte[] unless a write is called in between.
 * 
 * Note: once the InputStream is created, the output stream should
 * no longer be used.  In particular, make sure not to call reset()
 * and then write as that may overwrite the data that the InputStream
 * is using.
 */
public class LoadingByteArrayOutputStream extends ByteArrayOutputStream {
    public LoadingByteArrayOutputStream() {
        super(1024);
    }
    public LoadingByteArrayOutputStream(int i) {
        super(i);
    }
    
    private static class LoadedByteArrayInputStream extends ByteArrayInputStream implements Transferable {
        public LoadedByteArrayInputStream(byte[] buf, int length) {
            super(buf, 0, length);
        }
        @Override
        public String toString() {
            try {
                return new String(buf, 0, count, "utf-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(
                    "Impossible failure: Charset.forName(\"utf-8\") returns invalid name.");
            }
        }

        @Override
        public void transferTo(File file) throws IOException {
            FileOutputStream fout = new FileOutputStream(file);
            FileChannel channel = fout.getChannel();
            ByteBuffer bb = ByteBuffer.wrap(buf, 0, count); 
            while (bb.hasRemaining()) {
                channel.write(bb);
            }
            channel.close();
            fout.close();
        }
        
    }
    
    public ByteArrayInputStream createInputStream() {
        return new LoadedByteArrayInputStream(buf, count);
    }
    
    public void setSize(int i) {
        count = i;
    }
    
    @Override
    public byte[] toByteArray() {
        if (count != buf.length) {
            buf = super.toByteArray();
        }
        return buf;
    }
    
    public byte[] getRawBytes() {
        return buf;
    }
}