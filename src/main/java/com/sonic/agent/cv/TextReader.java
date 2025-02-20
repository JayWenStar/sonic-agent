package com.sonic.agent.cv;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.leptonica.PIX;
import org.bytedeco.tesseract.TessBaseAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static org.bytedeco.leptonica.global.lept.pixRead;

public class TextReader {
    private final Logger logger = LoggerFactory.getLogger(TextReader.class);

    public String getTessResult(File file, String language) throws Exception {
        BytePointer outText = null;
        TessBaseAPI api = new TessBaseAPI();
        String result = "";
        if (api.Init("language", language) != 0) {
            logger.info("找不到语言包！");
            return result;
        }
        try {
            PIX image = pixRead(file.getAbsolutePath());
            api.SetImage(image);
            outText = api.GetUTF8Text();
            result = outText.getString();
        } finally {
            file.delete();
            api.End();
            outText.deallocate();
        }
        return result;
    }
}
