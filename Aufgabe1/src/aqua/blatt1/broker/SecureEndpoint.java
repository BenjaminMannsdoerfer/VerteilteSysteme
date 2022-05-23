package aqua.blatt1.broker;

import messaging.Endpoint;
import messaging.Message;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class SecureEndpoint extends Endpoint {
    private final static String ALGORITHM = "AES";
    private final static String STRING_KEY = "CAFEBABECAFEBABE";
    private SecretKeySpec symKey;
    private final Endpoint endpoint;
    private Cipher encrypt;
    private Cipher decrypt;

    public SecureEndpoint() {
        endpoint = new Endpoint();
        initSecretKey();
        initEncryptCipher();
        initDecryptCipher();
    }

    public SecureEndpoint(int port) {
        endpoint = new Endpoint(port);
        initSecretKey();
        initEncryptCipher();
        initDecryptCipher();
    }

    @Override
    public void send(InetSocketAddress address, Serializable payload) {
        try {
            SealedObject sealedObject = new SealedObject(payload, encrypt);
            endpoint.send(address, sealedObject);
        } catch (IllegalBlockSizeException | IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Message blockingReceive() {
        Message message = endpoint.blockingReceive();
        return decryptMessage(message);
    }

    @Override
    public Message nonBlockingReceive() {
        Message message = endpoint.blockingReceive();
        return decryptMessage(message);
    }

    private Message decryptMessage(Message message) {
        SealedObject cryptoPayload = (SealedObject) message.getPayload();
        try {
            Serializable serializable = (Serializable) cryptoPayload.getObject(decrypt);

            return new Message(serializable, message.getSender());
        } catch (IllegalBlockSizeException | ClassNotFoundException | IOException | BadPaddingException e) {
            e.printStackTrace();
        }
        return null;
    }

    private SecretKeySpec initSecretKey() {
        symKey = new SecretKeySpec(STRING_KEY.getBytes(StandardCharsets.UTF_8), ALGORITHM);
        return symKey;
    }

    private void initEncryptCipher() {
        try {
            encrypt = Cipher.getInstance(ALGORITHM);
            if (symKey == null) {
                initSecretKey();
            }
            encrypt.init(Cipher.ENCRYPT_MODE, symKey);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
        }
    }

    private void initDecryptCipher() {
        try {
            decrypt = Cipher.getInstance(ALGORITHM);
            if (symKey == null) {
                initSecretKey();
            }
            decrypt.init(Cipher.DECRYPT_MODE, symKey);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
        }
    }
}