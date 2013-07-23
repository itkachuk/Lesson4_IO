package com.lohika.itkachuk.javatc.lesson4.client.ui;

import com.lohika.itkachuk.javatc.lesson4.server.FileTransferServer;

import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

/**
 * Created with IntelliJ IDEA.
 * User: itkachuk
 * Date: 6/13/13 5:21 PM
 */
public class FileTransferUIClient extends SwingWorker<Boolean, Integer> {
    private String serverHost;
    private int serverPort;
    private String filePath;
    private String targetPath;
    private FileTransferUIForm uiForm;

    public FileTransferUIClient(FileTransferUIForm uiForm) {
        this.uiForm = uiForm;
    }

    public void setData(String serverHost, Integer serverPort, String filePath, String targetPath) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.filePath = filePath;
        this.targetPath = targetPath;
    }

    public void validateInputs() throws IllegalArgumentException {
        if (isEmpty(serverHost)) {
            throw new IllegalArgumentException("Input error: server host name can not be empty");
        }
        if (serverPort < 1 || serverPort > 65535) {
            throw new IllegalArgumentException("Input error: server port should be from range 1-65535");
        }

        if (isEmpty(filePath)) {
            throw new IllegalArgumentException("Input error: file path can not be empty");
        }
        if (filePath.length() > 260) {
            throw new IllegalArgumentException("Input error: the total file path length can not be more than 260 characters");
        }

        File fileToSend = new File(filePath);
        if (!fileToSend.isFile()) {
            throw new IllegalArgumentException("Input error: \"" + filePath + "\" is not a file");
        }
        if (fileToSend.length() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Input error: file sizes more than \"" + Integer.MAX_VALUE/1048576 + "\" MB is not supported");
        }
        if (isNotEmpty(targetPath)) {
            if ((targetPath.length() + fileToSend.getName().length()) > 260) {
                throw new IllegalArgumentException("Input error: the total path length of target file can not be more than 260 characters");
            }
        }
    }

    // Copied source from Apache Commons lib, since we don't want to have any dependencies for this simple class
    private boolean isEmpty(CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    private boolean isNotEmpty(CharSequence cs) {
        return !isEmpty(cs);
    }
    //-------------------------------------------

    @Override
    protected Boolean doInBackground() throws Exception {
        File file = new File(filePath);
        long fileSize = file.length();
        long bytesInPercent = fileSize/100;
        ObjectInputStream ois = null;
        ObjectOutputStream oos = null;

        uiForm.getUploadButton().setEnabled(false);

        try {
            Socket socket = new Socket(serverHost, serverPort);
            ois = new ObjectInputStream(socket.getInputStream());
            oos = new ObjectOutputStream(socket.getOutputStream());

            uiForm.getCancelButton().setEnabled(true);

            oos.writeObject(file.getName());
            if (isNotEmpty(targetPath)) {
                oos.writeObject(targetPath);
            } else {
                oos.writeObject(FileTransferServer.DEFAULT_TARGET_PATH);
            }

            FileInputStream fis = new FileInputStream(file);
            byte [] buffer = new byte[FileTransferServer.BUFFER_SIZE];
            Integer bytesRead;
            Integer sentBytesCount = new Integer(0);
            int bytesInPercentCounter = 0;
            uiForm.getProgressBar().setMinimum(0);
            uiForm.getProgressBar().setMaximum((int)fileSize);
            if (fileSize < 524288) {
                uiForm.getUploadingStatus().setText("uploading file (" + fileSize/1024 + " KB)");
            } else {
                uiForm.getUploadingStatus().setText("uploading file (" + fileSize/1048576 + " MB)");
            }

            while (!isCancelled() && (bytesRead = fis.read(buffer)) > 0) {
                oos.writeObject(bytesRead);
                oos.writeObject(Arrays.copyOf(buffer, buffer.length));

                sentBytesCount = sentBytesCount + bytesRead;
                bytesInPercentCounter = bytesInPercentCounter + bytesRead;
                if (bytesInPercentCounter >= bytesInPercent) { // we don't want to update the UI each time, it will slow down the performance. Instead, each time when 1% of total file size is sent, we will update the progress bar.
                    uiForm.getProgressBar().setValue(sentBytesCount);
                    bytesInPercentCounter = 0;
                }
            }
            if (isCancelled()) {
                return false;
            }
            return true;
        } finally {
            try {
                oos.close();
                ois.close();
            } catch (Exception e) {}
        }
    }

    @Override
    public void done() {
        try {
            if (!isCancelled()) {
                Boolean result = get();
                if (result) {
                    uiForm.getProgressBar().setValue(uiForm.getProgressBar().getMaximum());
                    uiForm.getUploadingStatus().setText("file uploaded successfully");
                } else {
                    uiForm.getProgressBar().setValue(0);
                    uiForm.getUploadingStatus().setText("error occurred during file uploading");
                }
            } else {
                uiForm.getProgressBar().setValue(0);
                uiForm.getUploadingStatus().setText("file uploading was interrupted");
            }

        } catch (InterruptedException ie) {
            JOptionPane.showMessageDialog(uiForm,
                    ie.getMessage(),
                    "File uploading interrupted",
                    JOptionPane.ERROR_MESSAGE);
            //ie.printStackTrace();
        } catch (ExecutionException ee) {
            JOptionPane.showMessageDialog(uiForm,
                    ee.getMessage(),
                    "File uploading error",
                    JOptionPane.ERROR_MESSAGE);
            //ee.printStackTrace();
        } finally {
            uiForm.getUploadButton().setEnabled(true);
            uiForm.getCancelButton().setEnabled(false);
        }
    }


}
