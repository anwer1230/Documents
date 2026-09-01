# Google Inc. BoringCrypto FIPS 140-2 Security Policy

Software version: `24e5886c0edfc409c8083d10f9f1120111efd6f5`  
Date: July 18th, 2017  
Prepared by: Acumen Security (www.acumensecurity.net)

---

## 1. Introduction
Federal Information Processing Standards Publication 140-2 — Security Requirements for Cryptographic Modules specifies requirements for cryptographic modules to be deployed in a Sensitive but Unclassified environment.

Google Inc. BoringCrypto module is an open-source, general-purpose cryptographic library which provides FIPS 140-2 approved cryptographic algorithms to serve BoringSSL and other user-space applications. The validated version of the library is `24e5886c0edfc409c8083d10f9f1120111efd6f5`.

## 2. Tested Operational Environments
- Ubuntu Linux 14.04 LTS / Intel Xeon E5 / Clang 4.0.0
- Ubuntu Linux 16.04 / Intel Xeon E5 / Clang 4.0.0
- Ubuntu Linux 15.04 / POWER8 / Clang 4.0.0
- Ubuntu Linux 17.04 / POWER8 / Clang 4.0.0
- Ubuntu Linux 17.04 / POWER9 / Clang 4.0.0

## 3. Cryptographic Boundary
The physical cryptographic boundary is the general-purpose computer on which the module is installed. The logical cryptographic boundary is a single object file named `bcm.o` which is statically linked to BoringSSL.

## 4. Modes of Operation
- **Approved Mode**: When all power-up self-tests have completed successfully and only Approved algorithms are invoked.
- **Non-Approved Mode**: Entered when a non-Approved algorithm is invoked.

## 5. Approved Algorithms
- **AES**: CBC, ECB, CTR, GCM, KW (Cert #4558)
- **Triple-DES**: TCBC, TECB (Cert #2428)
- **ECDSA**: P-224, P-256, P-384, P-521 (Cert #1112, CVL #1240)
- **HMAC**: HMAC-SHA-1, HMAC-SHA-224, HMAC-SHA-256, HMAC-SHA-384, HMAC-SHA-512 (Cert #3011)
- **SHA**: SHA-1, SHA-224, SHA-256, SHA-384, SHA-512 (Cert #3736)
- **DRBG**: SP 800-90Arev1 CTR_DRBG (Cert #1507)
- **RSA**: 2048 to 16384 bits (Cert #2485)

## 6. Self-Tests
- **Power-On Self-Tests**: HMAC-SHA-512 Integrity Test, AES KAT, AES-GCM KAT, Triple-DES KAT, ECDSA KAT, HMAC KAT, CTR_DRBG KAT, RSA KAT, SHA KAT.
- **Conditional Self-Tests**: ECDSA Key Pair PCT, RSA Key Pair PCT, CRNGT on NDRNG, DRBG Health Tests.

## 7. Build and Verification
```bash
wget https://commondatastorage.googleapis.com/chromium-boringssl-docs/fips/boringssl-24e5886c0edfc409c8083d10f9f1120111efd6f5.tar.xz
sha256sum boringssl-24e5886c0edfc409c8083d10f9f1120111efd6f5.tar.xz
# Expected SHA-256: 15a65d676eeae27618e231183a1ce9804fc9c91bcc3abf5f6ca35216c02bf4da
```
