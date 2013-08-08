package org.jose4j.jwe.kdf;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jose4j.lang.ByteUtil;
import org.jose4j.lang.UncheckedJoseException;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 */
public class ConcatKeyDerivationFunction
{
    Log log = LogFactory.getLog(this.getClass());

    private int digestLength;
    private MessageDigest messageDigest;

    public ConcatKeyDerivationFunction(String hashAlgoritm)
    {
        messageDigest = getMessageDigest(hashAlgoritm);
        digestLength = ByteUtil.bitLength(messageDigest.getDigestLength());

        log.debug("Hash Algorithm: " + hashAlgoritm + " with hashlen: " + digestLength + " bits");
    }

    public byte[] kdf(byte[] sharedSecret, int keydatalen, byte[] algorithmId, byte[] partyUInfo, byte[] partyVInfo, byte[] suppPubInfo, byte[] suppPrivInfo)
    {
        log.debug("KDF:");
        log.debug("  z: " + ByteUtil.toDebugString(sharedSecret));
        log.debug("  keydatalen: " + keydatalen);
        log.debug("  algorithmId: " + ByteUtil.toDebugString(algorithmId));
        log.debug("  partyUInfo: " + ByteUtil.toDebugString(partyUInfo));
        log.debug("  suppPubInfo: " + ByteUtil.toDebugString(suppPubInfo));
        log.debug("  suppPrivInfo: " + ByteUtil.toDebugString(suppPrivInfo));

        byte[] otherInfo = ByteUtil.concat(algorithmId, partyUInfo, partyVInfo, suppPubInfo, suppPrivInfo);
        return kdf(sharedSecret, keydatalen, otherInfo);
    }

    public byte[] kdf(byte[] sharedSecret, int keydatalen, byte[] otherInfo)
    {
        long reps = getReps(keydatalen);
        log.debug("reps: " + reps);
        log.debug("otherInfo: " + ByteUtil.toDebugString(otherInfo));

        ByteArrayOutputStream derivedByteOutputStream = new ByteArrayOutputStream();
        for (int i = 1; i <= reps; i++)
        {
            log.debug("rep " + i + " hashing ");
            byte[] counterBytes = ByteUtil.getBytes(i);
            log.debug(" counter: " + ByteUtil.toDebugString(otherInfo));
            log.debug(" z: " + ByteUtil.toDebugString(sharedSecret));
            log.debug(" otherInfo: " + ByteUtil.toDebugString(otherInfo));

            messageDigest.update(counterBytes);
            messageDigest.update(sharedSecret);
            messageDigest.update(otherInfo);
            byte[] digest = messageDigest.digest();
            log.debug(" k("+i+ "): " + ByteUtil.toDebugString(digest));
            derivedByteOutputStream.write(digest, 0, digest.length);
        }

        int keyDateLenInBytes = ByteUtil.getNumberOfBytes(keydatalen);
        byte[] derivedKeyMaterial = derivedByteOutputStream.toByteArray();
        log.debug("derived key material: " + ByteUtil.toDebugString(derivedKeyMaterial));
        if (derivedKeyMaterial.length != keyDateLenInBytes)
        {
            byte[] newKeyMaterial = new byte[keyDateLenInBytes];
            System.arraycopy(derivedKeyMaterial, 0, newKeyMaterial, 0, keyDateLenInBytes);
            log.debug("first "+keydatalen+" bits of derived key material: " + ByteUtil.toDebugString(newKeyMaterial));
            derivedKeyMaterial = newKeyMaterial;
        }

        log.debug("final derived key material: " + ByteUtil.toDebugString(derivedKeyMaterial));
        return derivedKeyMaterial;
    }

    long getReps(int keydatalen)
    {
        double repsD = (float) keydatalen / (float) digestLength;
        repsD = Math.ceil(repsD);
        return Math.round(repsD);
    }

    private MessageDigest getMessageDigest(String digestMethod)
    {
       try
       {
          return MessageDigest.getInstance(digestMethod);
       }
       catch (NoSuchAlgorithmException e)
       {
           throw new UncheckedJoseException("Must have " + digestMethod + " but don't.", e);
       }
    }
}
