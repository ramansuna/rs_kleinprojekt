package RS_KleinProjekt.rs_kleinprojekt;

import java.nio.file.Path;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;


public class FileEncryptorController {

    // Konstanten für die Verschlüsselung
    private static final int KEY_LENGTH = 256;
    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String KEY_ALGORITHM = "PBKDF2WithHmacSHA512";
    private static final int ITERATIONS = 65536;
    private static final int SALT_LENGTH = 16;
    private static final int IV_LENGTH = 16;

    @FXML
    private GridPane rootPane;
    @FXML
    private VBox mainContainer;
    @FXML
    private Label titleLabel;
    @FXML
    private HBox inputFilePathContainer;
    @FXML
    private Label inputFilePathLabel;
    @FXML
    private TextField inputFilePath;
    @FXML
    private HBox outputFilePathContainer;
    @FXML
    private Label outputFilePathLabel;
    @FXML
    private TextField outputFilePath;
    @FXML
    private HBox passwordContainer;
    @FXML
    private Label passwordLabel;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Button encryptButton;
    @FXML
    private Button decryptButton;
    @FXML
    private Rectangle separator;
    @FXML
    private Label statusLabel;
    @FXML
    private Rectangle inputFilePathDropArea;
    @FXML
    private Rectangle outputFilePathDropArea;

    @FXML
    public void encryptFile() {
        try {
            String inputPath = inputFilePath.getText();
            String outputPath = outputFilePath.getText();
            String password = passwordField.getText();

            Path inputFile = Path.of(inputPath);
            Path outputFile = Path.of(outputPath);

            encrypt(inputFile, outputFile, password);
            statusLabel.setText("Encryption successful");
        } catch (Exception e) {
            statusLabel.setText("Encryption failed: " + e.getMessage());
        }
    }

    @FXML
    public void decryptFile() {
        try {
            String inputPath = inputFilePath.getText();
            String outputPath = outputFilePath.getText();
            String password = passwordField.getText();

            Path inputFile = Path.of(inputPath);
            Path outputFile = Path.of(outputPath);

            decrypt(inputFile, outputFile, password);
            statusLabel.setText("Decryption successful");
        } catch (Exception e) {
            statusLabel.setText("Decryption failed: " + e.getMessage());
        }
    }

    @FXML
    public void handleDragOver(DragEvent event) {
        if (event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY);
        }
        event.consume();
    }
    
    @FXML
    public void handleDragDropped(DragEvent event) {
        if (event.getDragboard().hasFiles()) {
            String filePath = event.getDragboard().getFiles().get(0).getPath();
            inputFilePath.setText(filePath);
        }
        event.setDropCompleted(true);
        event.consume();
    }

    

    @FXML
    public void handleInputFileDropped(DragEvent event) {
        if (event.getDragboard().hasFiles()) {
            String filePath = event.getDragboard().getFiles().get(0).getPath();
            inputFilePath.setText(filePath);
        }
        event.setDropCompleted(true);
        event.consume();
    }

    @FXML
    public void handleOutputFileDropped(DragEvent event) {
        if (event.getDragboard().hasFiles()) {
            String filePath = event.getDragboard().getFiles().get(0).getPath();
            outputFilePath.setText(filePath);
        }
        event.setDropCompleted(true);
        event.consume();
    }

    private void encrypt(Path inputFile, Path outputFile, String password)
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException,
            InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {

        byte[] salt = generateSalt();
        SecretKey secretKey = generateSecretKey(password, salt);
        byte[] iv = generateIV();

        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream);
                CipherOutputStream cipherOutputStream = new CipherOutputStream(gzipOutputStream, cipher);
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(cipherOutputStream);
                FileInputStream fileInputStream = new FileInputStream(inputFile.toFile());
                BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
                bufferedOutputStream.write(buffer, 0, bytesRead);
            }
            bufferedOutputStream.flush();

            byte[] encryptedData = outputStream.toByteArray();

            try (DataOutputStream fileOutputStream = new DataOutputStream(new FileOutputStream(outputFile.toFile()))) {
                fileOutputStream.write(salt);
                fileOutputStream.write(iv);
                fileOutputStream.writeInt(encryptedData.length);
                fileOutputStream.write(encryptedData);
            }
        }
    }

    private void decrypt(Path inputFile, Path outputFile, String password)
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException,
            InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {

        try (DataInputStream fileInputStream = new DataInputStream(new FileInputStream(inputFile.toFile()))) {
            byte[] salt = new byte[SALT_LENGTH];
            fileInputStream.readFully(salt);
            byte[] iv = new byte[IV_LENGTH];
            fileInputStream.readFully(iv);
            int encryptedDataLength = fileInputStream.readInt();
            byte[] encryptedData = new byte[encryptedDataLength];
            fileInputStream.readFully(encryptedData);

            SecretKey secretKey = generateSecretKey(password, salt);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));

            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(encryptedData);
                    CipherInputStream cipherInputStream = new CipherInputStream(inputStream, cipher);
                    GZIPInputStream gzipInputStream = new GZIPInputStream(cipherInputStream);
                    BufferedInputStream bufferedInputStream = new BufferedInputStream(gzipInputStream);
                    FileOutputStream fileOutputStream = new FileOutputStream(outputFile.toFile());
                    BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream)) {

                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
                    bufferedOutputStream.write(buffer, 0, bytesRead);
                }
                bufferedOutputStream.flush();
            }
        }
    }

    private byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(salt);
        return salt;
    }

    private SecretKey generateSecretKey(String password, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_ALGORITHM);
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        SecretKey secretKey = factory.generateSecret(spec);
        return new SecretKeySpec(secretKey.getEncoded(), "AES");
    }

    private byte[] generateIV() {
        byte[] iv = new byte[IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        return iv;
    }
}