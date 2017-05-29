package ru.elmsoft.sms.transport.file;

import java.io.File;
import java.util.*;

import org.apache.log4j.Logger;

public class CheckFileService implements Runnable {
    private String nam_file = "test.txt";
    private Logger logger = null;
    private List list = new LinkedList();
    public void run() {
        File file = new File(nam_file);
        while(true){
            if (file.exists()){
              //  LOGGER.info("File exist:" + nam_file);
                sendNotify(true);
            }else {
               // LOGGER.info("File not exist:" + nam_file);
                sendNotify(false);
            }
            synchronized (this) {
                try {
                    nam_file.wait(1000);
                } catch (InterruptedException err) {
                    err.printStackTrace();
                }
            }
        }
    }
    
    public void addListener(FileServiceListener listener){
        synchronized (list) {
            list.add(listener);
        }
    }
    
    private void sendNotify(boolean value){
        for (Iterator iter = list.iterator(); iter.hasNext();) {
            FileServiceListener element = (FileServiceListener) iter.next();
            element.fileEvent(value);
        }
    }
    public void init(Properties props){
        nam_file = props.getProperty("test.file");
        logger = Logger.getLogger(getClass());
        logger.debug("Value property test.file=>" + nam_file);
    }
}
