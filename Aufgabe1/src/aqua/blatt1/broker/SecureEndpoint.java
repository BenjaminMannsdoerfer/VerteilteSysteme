package aqua.blatt1.broker;

import aqua.blatt1.common.msgtypes.KeyExchangeMessage;
import messaging.Endpoint;
import messaging.Message;

import javax.crypto.*;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.security.*;
import java.util.HashMap;
import java.util.Map;

public class SecureEndpoint extends Endpoint {
    private final static String ALGORITHM = "RSA";
    private KeyPairGenerator keyPairGenerator;
    private final Endpoint endpoint;
    private Cipher encrypt;
    private Cipher decrypt;
    private PublicKey publicKey;
    private PrivateKey privateKey;
    private Map<InetSocketAddress, PublicKey> connectors;

    public SecureEndpoint() throws NoSuchAlgorithmException {
        endpoint = new Endpoint();
        connectors = new HashMap<>();
        initKeys();
        initEncryptCipher();
        initDecryptCipher();
    }

    public SecureEndpoint(int port) throws NoSuchAlgorithmException {
        endpoint = new Endpoint(port);
        connectors = new HashMap<>();
        initKeys();
        initEncryptCipher();
        initDecryptCipher();
    }

    @Override
    public void send(InetSocketAddress address, Serializable payload) {
        try {
            if (!this.connectors.containsKey(address)) {
                this.endpoint.send(address, new KeyExchangeMessage(this.publicKey));
            }
            this.endpoint.send(address, new SealedObject(payload, this.encrypt));
        } catch (IOException | IllegalBlockSizeException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Message blockingReceive() {
        Message message = endpoint.blockingReceive();
        return handleMessage(message);
    }

    @Override
    public Message nonBlockingReceive() {
        Message message = endpoint.blockingReceive();
        return handleMessage(message);
    }

    private Message handleMessage(Message message) {
        if (message != null && message.getPayload() instanceof KeyExchangeMessage) {
            InetSocketAddress address = message.getSender();
            if (!this.connectors.containsKey(address)) {
                this.connectors.put(address, ((KeyExchangeMessage) message.getPayload()).getPublicKey());
                this.endpoint.send(address, new KeyExchangeMessage(this.publicKey));
            }
            return null;
        }
        if (message == null) {
            return null;
        }
        return decryptMessage(message);
    }

    private Message decryptMessage(Message message) {
        if (message == null) {
            return null;
        }
        if (!this.connectors.containsKey(message.getSender())) {
            this.endpoint.send(message.getSender(), new KeyExchangeMessage(this.publicKey));
            return null;
        }
        PublicKey publicKey = this.connectors.get(message.getSender());
        SealedObject cryptoPayload = (SealedObject) message.getPayload();
        try {
            Serializable serializable = (Serializable) cryptoPayload.getObject(publicKey);
            return new Message(serializable, message.getSender());
        } catch (ClassNotFoundException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void initEncryptCipher() {
        try {
            encrypt = Cipher.getInstance(ALGORITHM);
            if (keyPairGenerator == null) {
                initKeys();
            }
            encrypt.init(Cipher.ENCRYPT_MODE, this.privateKey);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
        }
    }

    private void initDecryptCipher() {
        try {
            decrypt = Cipher.getInstance(ALGORITHM);
            if (keyPairGenerator == null) {
                initKeys();
            }
            decrypt.init(Cipher.DECRYPT_MODE, this.publicKey);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
        }
    }

    private void initKeys() throws NoSuchAlgorithmException {
        keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM);
        keyPairGenerator.initialize(4096);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        this.publicKey = keyPair.getPublic();
        this.privateKey = keyPair.getPrivate();
    }
}