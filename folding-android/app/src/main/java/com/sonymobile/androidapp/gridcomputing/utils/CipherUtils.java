/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.utils;

import android.security.KeyPairGeneratorSpec;
import android.util.Base64;

import com.sonymobile.androidapp.gridcomputing.log.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Calendar;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.security.auth.x500.X500Principal;

/**
 * Utility class to encrypt/decrypt strings.
 */
public final class CipherUtils {

    /**
     * Cipher transformation used to encrypt/descrypt.
     */
    private static final String CIPHER_TRANSFORMATION = "RSA/ECB/PKCS1Padding";

    /**
     * The cipher provider.
     */
    private static final String CIPHER_PROVIDER = "AndroidOpenSSL";

    /**
     * Char encoding used on cipher operations.
     */
    private static final String CIPHER_ENCODING = "UTF-8";

    /**
     * The key provider.
     */
    private static final String KEY_PROVIDER = "AndroidKeyStore";

    /**
     * The key algorithm.
     */
    private static final String KEY_ALGORITHM = "RSA";

    /**
     * The key alias.
     */
    private static final String KEY_ALIAS = "com.sonymobile.androidapp.gridcomputing.KEY_ALIAS";

    /**
     * The key subject.
     */
    private static final String KEY_SUBJECT = "CN=FoldingAtHome, O=FoldingAtHome";

    /**
     * The key validity in years.
     */
    private static final int KEY_VALIDITY_YEARS = 20;

    /**
     * KeyStore object used in cipher operations.
     */
    private static KeyStore sKeyStore;

    static {
        try {
            sKeyStore = KeyStore.getInstance(KEY_PROVIDER);
            sKeyStore.load(null);
        } catch (GeneralSecurityException | IOException ex) {
            Log.e(ex.getMessage());
        }
    }

    /**
     * This class is not intended to be instantiated.
     */
    private CipherUtils() {
    }

    /**
     * Checks it a keystore was created.
     *
     * @return true if the keystore exists.
     */
    private static boolean hasKey() {
        try {
            return sKeyStore.containsAlias(KEY_ALIAS);
        } catch (KeyStoreException | NullPointerException ex) {
            Log.e(ex.getMessage());
            return false;
        }
    }

    /**
     * Creates a new keystore.
     */
    private static void createKey() {
        try {
            Calendar start = Calendar.getInstance();
            Calendar end = Calendar.getInstance();
            end.add(Calendar.YEAR, KEY_VALIDITY_YEARS);
            KeyPairGeneratorSpec spec =
                    new KeyPairGeneratorSpec.Builder(ApplicationData.getAppContext())
                            .setAlias(KEY_ALIAS)
                            .setSubject(new X500Principal(KEY_SUBJECT))
                            .setSerialNumber(BigInteger.ONE)
                            .setStartDate(start.getTime())
                            .setEndDate(end.getTime())
                            .build();
            KeyPairGenerator generator = KeyPairGenerator.getInstance(KEY_ALGORITHM, KEY_PROVIDER);
            generator.initialize(spec);
            generator.generateKeyPair();
        } catch (GeneralSecurityException | IllegalStateException ex) {
            Log.e(ex.getMessage());
        }
    }

    /**
     * Encrypts a word.
     *
     * @param word the String to encrypt.
     * @return the Base64 encrypted word.
     */
    public static String encrypt(final String word) {
        if (!hasKey()) {
            createKey();
        }
        try {
            final KeyStore.PrivateKeyEntry privateKeyEntry =
                    (KeyStore.PrivateKeyEntry) sKeyStore.getEntry(KEY_ALIAS, null);
            final RSAPublicKey publicKey =
                    (RSAPublicKey) privateKeyEntry.getCertificate().getPublicKey();

            final Cipher input = Cipher.getInstance(CIPHER_TRANSFORMATION, CIPHER_PROVIDER);
            input.init(Cipher.ENCRYPT_MODE, publicKey);

            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            final CipherOutputStream cipherOutputStream = new CipherOutputStream(
                    outputStream, input);
            cipherOutputStream.write(word.getBytes(CIPHER_ENCODING));
            cipherOutputStream.close();

            return (Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT));
        } catch (Exception ex) {
            Log.e(ex.getMessage());
            return word;
        }
    }

    /**
     * Decrypts a word.
     *
     * @param word the Base64 encrypted word.
     * @return the decrypted String.
     */
    public static String decrypt(final String word) {
        if (!hasKey()) {
            createKey();
        }
        try {
            final KeyStore.PrivateKeyEntry entry =
                    (KeyStore.PrivateKeyEntry) sKeyStore.getEntry(KEY_ALIAS, null);
            KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) entry;

            Cipher output = Cipher.getInstance(CIPHER_TRANSFORMATION);
            output.init(Cipher.DECRYPT_MODE, privateKeyEntry.getPrivateKey());

            final CipherInputStream cipherInputStream = new CipherInputStream(
                    new ByteArrayInputStream(Base64.decode(word, Base64.DEFAULT)), output);
            final ArrayList<Byte> values = new ArrayList<>();
            int nextByte;
            while ((nextByte = cipherInputStream.read()) != -1) {
                values.add((byte) nextByte);
            }

            final byte[] bytes = new byte[values.size()];
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = values.get(i);
            }

            return new String(bytes, 0, bytes.length, CIPHER_ENCODING);
        } catch (Exception ex) {
            Log.e(ex.getMessage());
            return word;
        }
    }
}
