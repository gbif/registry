/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.ws.it.collections.service.batch;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipUtils {

  static List<Path> unzip(Path zipFilePath, String destDir) {
    List<Path> filesUnzipped = new ArrayList<>();
    File dir = new File(destDir);
    // create output directory if it doesn't exist
    if (!dir.exists()) {
      dir.mkdirs();
    }
    FileInputStream fis;
    // buffer for read and write data to file
    byte[] buffer = new byte[1024];
    try {
      fis = new FileInputStream(zipFilePath.toFile());
      ZipInputStream zis = new ZipInputStream(fis);
      ZipEntry ze = zis.getNextEntry();
      while (ze != null) {
        String fileName = ze.getName();
        File newFile = new File(destDir + File.separator + fileName);
        filesUnzipped.add(newFile.toPath());
        // create directories for sub directories in zip
        new File(newFile.getParent()).mkdirs();
        FileOutputStream fos = new FileOutputStream(newFile);
        int len;
        while ((len = zis.read(buffer)) > 0) {
          fos.write(buffer, 0, len);
        }
        fos.close();
        // close this ZipEntry
        zis.closeEntry();
        ze = zis.getNextEntry();
      }
      // close last ZipEntry
      zis.closeEntry();
      zis.close();
      fis.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return filesUnzipped;
  }

}
