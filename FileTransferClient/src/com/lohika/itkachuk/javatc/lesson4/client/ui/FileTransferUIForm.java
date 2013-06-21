package com.lohika.itkachuk.javatc.lesson4.client.ui;

import com.lohika.itkachuk.javatc.lesson4.client.FileTransferClient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: itkachuk
 * Date: 6/13/13 5:26 PM
 */
public class FileTransferUIForm extends JFrame {
    private JButton browseButton;
    private JButton uploadButton;
    private JTextField filePath;
    private JTextField targerPath;
    private JTextField serverHost;
    private JSpinner serverPort;
    private JPanel mainPanel;
    private JProgressBar progressBar;
    private JButton cancelButton;
    private JLabel uploadingStatus;
    private JFileChooser fileChooser;
    private FileTransferUIClient fileTransferUIClient;

    public JButton getUploadButton() {
        return uploadButton;
    }

    public JProgressBar getProgressBar() {
        return progressBar;
    }

    public JButton getCancelButton() {
        return cancelButton;
    }

    public JLabel getUploadingStatus() {
        return uploadingStatus;
    }


    public FileTransferUIForm() {
        super();

        // default values for Server
        serverHost.setText("localhost");
        serverPort.setValue(9999);

        fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        progressBar.setStringPainted(true);

        addActionListeners();

        this.setTitle("File Transfer Client");
        this.setContentPane(mainPanel);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setMinimumSize(new Dimension(600, 280));
        this.pack();
        this.setVisible(true);
    }

    private void addActionListeners() {
        browseButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int returnVal = fileChooser.showOpenDialog(FileTransferUIForm.this);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    filePath.setText(file.getAbsolutePath());
                }
            }
        });

        uploadButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {

                try {
                    fileTransferUIClient = new FileTransferUIClient(FileTransferUIForm.this);
                    fileTransferUIClient.setData(serverHost.getText(), (Integer) serverPort.getValue(), filePath.getText(), targerPath.getText());
                    fileTransferUIClient.validateInputs();
                    fileTransferUIClient.execute();
            /*    } catch (IOException ioe) {
                    JOptionPane.showMessageDialog(FileTransferUIForm.this,
                            ioe.getMessage(),
                            "File uploading error",
                            JOptionPane.ERROR_MESSAGE); */
                } catch (IllegalArgumentException iae) {
                    JOptionPane.showMessageDialog(FileTransferUIForm.this,
                            iae.getMessage(),
                            "Inputs validation error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        cancelButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fileTransferUIClient.cancel(true);
            }
        });
    }

    public static void main(String[] args) {
        // set L&F
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
        }

        FileTransferUIForm fileTransferUIForm = new FileTransferUIForm();
    }
}
