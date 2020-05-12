#!/bin/bash
echo install git-crypt
sudo apt-get install -qq git-crypt
echo decode git-crypt-key
echo $GIT_CRYPT_KEY_BASE64 | base64 --decode > ../.git-crypt-key
echo unlock repository
git-crypt unlock ../.git-crypt-key
