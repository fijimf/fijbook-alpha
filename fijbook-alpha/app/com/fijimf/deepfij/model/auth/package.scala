package com.fijimf.deepfij.model


/*
create table `user` (`userID` VARCHAR(144) NOT NULL PRIMARY KEY,`firstName` TEXT,`lastName` TEXT,`fullName` TEXT,`email` VARCHAR(144),`avatarURL` TEXT,`activated` BOOLEAN NOT NULL);
create unique index `user_idx1` on `user` (`email`);
create table `logininfo` (`id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,`providerID` VARCHAR(144) NOT NULL,`providerKey` VARCHAR(144) NOT NULL);
create unique index `logininfos_idx1` on `logininfo` (`providerID`,`providerKey`);
create table `userlogininfo` (`userID` VARCHAR(144) NOT NULL,`loginInfoID` BIGINT NOT NULL);
create unique index `ulijoin_idx1` on `userlogininfo` (`userID`);
create unique index `ulijoin_idx2` on `userlogininfo` (`loginInfoID`);
create table `passwordinfo` (`hasher` TEXT NOT NULL,`password` TEXT NOT NULL,`salt` TEXT,`loginInfoId` BIGINT NOT NULL);

 */
package object auth {

}
