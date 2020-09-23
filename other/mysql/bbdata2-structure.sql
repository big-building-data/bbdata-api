-- MySQL Script generated by MySQL Workbench
-- Wed Sep 23 16:33:54 2020
-- Model: New Model    Version: 1.0
-- MySQL Workbench Forward Engineering

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- -----------------------------------------------------
-- Schema bbdata2
-- -----------------------------------------------------
DROP SCHEMA IF EXISTS `bbdata2` ;

-- -----------------------------------------------------
-- Schema bbdata2
-- -----------------------------------------------------
CREATE SCHEMA IF NOT EXISTS `bbdata2` DEFAULT CHARACTER SET utf8 ;
USE `bbdata2` ;

-- -----------------------------------------------------
-- Table `users`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `users` ;

CREATE TABLE IF NOT EXISTS `users` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(45) NOT NULL,
  `email` VARCHAR(45) NOT NULL,
  `password` VARCHAR(45) NOT NULL,
  `creationdate` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `name_UNIQUE` (`name` ASC) VISIBLE)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `apikeys`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `apikeys` ;

CREATE TABLE IF NOT EXISTS `apikeys` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `user_id` INT(11) NOT NULL,
  `secret` VARCHAR(32) NOT NULL,
  `readonly` TINYINT(1) NOT NULL DEFAULT '1',
  `expirationdate` DATETIME NULL DEFAULT NULL,
  `description` VARCHAR(65) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  INDEX `fk_apikey_user1_idx` (`user_id` ASC) VISIBLE,
  CONSTRAINT `fk_apikey_user1`
    FOREIGN KEY (`user_id`)
    REFERENCES `users` (`id`)
    ON DELETE CASCADE
    ON UPDATE NO ACTION)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `types`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `types` ;

CREATE TABLE IF NOT EXISTS `types` (
  `name` VARCHAR(45) NOT NULL,
  PRIMARY KEY (`name`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `units`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `units` ;

CREATE TABLE IF NOT EXISTS `units` (
  `symbol` VARCHAR(10) NOT NULL,
  `name` VARCHAR(20) NOT NULL,
  `type` VARCHAR(45) NOT NULL,
  PRIMARY KEY (`symbol`),
  INDEX `fk_unit_type1_idx` (`type` ASC) VISIBLE,
  UNIQUE INDEX `name_UNIQUE` (`name` ASC) VISIBLE,
  CONSTRAINT `fk_unit_type1`
    FOREIGN KEY (`type`)
    REFERENCES `types` (`name`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `ugrps`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `ugrps` ;

CREATE TABLE IF NOT EXISTS `ugrps` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(45) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `name_UNIQUE` (`name` ASC) VISIBLE)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `objects`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `objects` ;

CREATE TABLE IF NOT EXISTS `objects` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(60) NOT NULL,
  `description` VARCHAR(255) NULL DEFAULT NULL,
  `creationdate` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `disabled` TINYINT(1) NOT NULL DEFAULT '0',
  `ugrp_id` INT(11) NOT NULL,
  `unit_symbol` VARCHAR(10) NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `fk_object_user_grp1_idx` (`ugrp_id` ASC) VISIBLE,
  INDEX `fk_object_unit1_idx` (`unit_symbol` ASC) VISIBLE,
  CONSTRAINT `fk_object_unit1`
    FOREIGN KEY (`unit_symbol`)
    REFERENCES `units` (`symbol`)
    ON DELETE NO ACTION
    ON UPDATE CASCADE,
  CONSTRAINT `fk_object_user_grp1`
    FOREIGN KEY (`ugrp_id`)
    REFERENCES `ugrps` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `comments`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `comments` ;

CREATE TABLE IF NOT EXISTS `comments` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `object_id` INT(11) NOT NULL,
  `dfrom` DATETIME NOT NULL,
  `dto` DATETIME NOT NULL,
  `comment` TEXT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `id_UNIQUE` (`id` ASC) VISIBLE,
  INDEX `fk_comments_objects_idx` (`object_id` ASC) VISIBLE,
  CONSTRAINT `fk_comments_objects`
    FOREIGN KEY (`object_id`)
    REFERENCES `objects` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `ogrps`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `ogrps` ;

CREATE TABLE IF NOT EXISTS `ogrps` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(45) NOT NULL,
  `description` VARCHAR(255) NULL DEFAULT NULL,
  `ugrp_id` INT(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `name_UNIQUE` (`name` ASC) VISIBLE,
  INDEX `fk_sensor_grp_user_grp1_idx` (`ugrp_id` ASC) VISIBLE,
  CONSTRAINT `fk_sensor_grp_user_grp1`
    FOREIGN KEY (`ugrp_id`)
    REFERENCES `ugrps` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `objects_ogrps`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `objects_ogrps` ;

CREATE TABLE IF NOT EXISTS `objects_ogrps` (
  `ogrp_id` INT(11) NOT NULL,
  `object_id` INT(11) NOT NULL,
  PRIMARY KEY (`ogrp_id`, `object_id`),
  INDEX `fk_sensor_has_sensor_grp_sensor_grp1_idx` (`ogrp_id` ASC) VISIBLE,
  INDEX `fk_sensor_has_sensor_grp_sensor1_idx` (`object_id` ASC) VISIBLE,
  CONSTRAINT `fk_sensor_has_sensor_grp_sensor1`
    FOREIGN KEY (`object_id`)
    REFERENCES `objects` (`id`)
    ON DELETE CASCADE
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_sensor_has_sensor_grp_sensor_grp1`
    FOREIGN KEY (`ogrp_id`)
    REFERENCES `ogrps` (`id`)
    ON DELETE CASCADE
    ON UPDATE NO ACTION)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `rights`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `rights` ;

CREATE TABLE IF NOT EXISTS `rights` (
  `ogrp_id` INT(11) NOT NULL,
  `ugrp_id` INT(11) NOT NULL,
  PRIMARY KEY (`ogrp_id`, `ugrp_id`),
  INDEX `fk_sensor_grp_has_user_grp_user_grp1_idx` (`ugrp_id` ASC) VISIBLE,
  INDEX `fk_sensor_grp_has_user_grp_sensor_grp1_idx` (`ogrp_id` ASC) VISIBLE,
  CONSTRAINT `fk_sensor_grp_has_user_grp_sensor_grp1`
    FOREIGN KEY (`ogrp_id`)
    REFERENCES `ogrps` (`id`)
    ON DELETE CASCADE
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_sensor_grp_has_user_grp_user_grp1`
    FOREIGN KEY (`ugrp_id`)
    REFERENCES `ugrps` (`id`)
    ON DELETE CASCADE
    ON UPDATE NO ACTION)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `tags`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `tags` ;

CREATE TABLE IF NOT EXISTS `tags` (
  `name` VARCHAR(25) NOT NULL,
  `object_id` INT(11) NOT NULL,
  PRIMARY KEY (`name`, `object_id`),
  INDEX `fk_tags_objects1_idx` (`object_id` ASC) VISIBLE,
  CONSTRAINT `fk_tags_objects1`
    FOREIGN KEY (`object_id`)
    REFERENCES `objects` (`id`)
    ON DELETE CASCADE
    ON UPDATE NO ACTION)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `tokens`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `tokens` ;

CREATE TABLE IF NOT EXISTS `tokens` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `token` VARCHAR(32) NOT NULL,
  `object_id` INT(11) NOT NULL,
  `description` VARCHAR(65) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `token_UNIQUE` (`token` ASC) VISIBLE,
  INDEX `fk_token_object1_idx` (`object_id` ASC) VISIBLE,
  CONSTRAINT `fk_token_object1`
    FOREIGN KEY (`object_id`)
    REFERENCES `objects` (`id`)
    ON DELETE CASCADE
    ON UPDATE NO ACTION)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `userperms`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `userperms` ;

CREATE TABLE IF NOT EXISTS `userperms` (
  `user_id` INT(11) NOT NULL,
  `object_id` INT(11) NOT NULL,
  `writable` TINYINT(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`user_id`, `object_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `users_ugrps`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `users_ugrps` ;

CREATE TABLE IF NOT EXISTS `users_ugrps` (
  `user_id` INT(11) NOT NULL,
  `ugrp_id` INT(11) NOT NULL,
  `is_admin` TINYINT(1) NULL DEFAULT '0',
  PRIMARY KEY (`user_id`, `ugrp_id`),
  INDEX `fk_user_has_user_grp_user_grp1_idx` (`ugrp_id` ASC) VISIBLE,
  INDEX `fk_user_has_user_grp_user1_idx` (`user_id` ASC) VISIBLE,
  CONSTRAINT `fk_user_has_user_grp_user1`
    FOREIGN KEY (`user_id`)
    REFERENCES `users` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_user_has_user_grp_user_grp1`
    FOREIGN KEY (`ugrp_id`)
    REFERENCES `ugrps` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `stats`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `stats` ;

CREATE TABLE IF NOT EXISTS `stats` (
  `object_id` INT(11) NOT NULL,
  `n_reads` INT NOT NULL DEFAULT 0,
  `n_writes` INT NOT NULL DEFAULT 0,
  `last_ts` TIMESTAMP(3) NOT NULL,
  `avg_sample_period` DOUBLE NOT NULL DEFAULT 0,
  `created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `last_modified` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`object_id`),
  CONSTRAINT `fk_stats_objects1`
    FOREIGN KEY (`object_id`)
    REFERENCES `objects` (`id`)
    ON DELETE CASCADE
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

USE `bbdata2` ;

-- -----------------------------------------------------
-- Placeholder table for view `ogrps_read`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `ogrps_read` (`user_id` INT, `ogrp_id` INT);

-- -----------------------------------------------------
-- Placeholder table for view `ogrps_write`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `ogrps_write` (`user_id` INT, `ogrp_id` INT);

-- -----------------------------------------------------
-- procedure login
-- -----------------------------------------------------

USE `bbdata2`;
DROP procedure IF EXISTS `login`;

DELIMITER $$
USE `bbdata2`$$
CREATE PROCEDURE `login`(IN userId INT, IN clearpass VARCHAR(45), OUT ok BOOL)
BEGIN
	DECLARE pass VARCHAR(45);
    SELECT MD5(clearpass) INTO pass;
    SELECT COUNT(id) > 0 FROM users WHERE id = userId AND password = pass INTO ok;
END$$

DELIMITER ;

-- -----------------------------------------------------
-- procedure rebuild_userperms
-- -----------------------------------------------------

USE `bbdata2`;
DROP procedure IF EXISTS `rebuild_userperms`;

DELIMITER $$
USE `bbdata2`$$
CREATE PROCEDURE `rebuild_userperms`()
BEGIN
    DELETE FROM userperms; -- don't use truncate: aises an error when called from a trigger
    INSERT IGNORE INTO userperms 
    SELECT DISTINCT * FROM (
		-- all admin members of the ugrp owner of the object have write permission
        SELECT ug.user_id, o.id, ug.is_admin 
        FROM users_ugrps ug INNER JOIN objects o ON o.ugrp_id = ug.ugrp_id
        UNION
        -- all member of group admin have write permission
        -- SELECT ug.user_id, o.id, 1
        -- FROM users_ugrps ug, objects o
        -- WHERE ug.ugrp_id = 1 and ug.is_admin = true
        -- UNION
        -- all members of an ugrp added to an object group having the object have read permission
        SELECT ug.user_id, og.object_id, 0
        FROM users_ugrps ug
        INNER JOIN rights r ON ug.ugrp_id = r.ugrp_id 
        INNER JOIN objects_ogrps og ON r.ogrp_id = og.ogrp_id
        INNER JOIN objects o ON og.object_id = o.id
        WHERE ug.ugrp_id <> o.ugrp_id
    ) tmp;
END$$

DELIMITER ;

-- -----------------------------------------------------
-- procedure clean_apikeys
-- -----------------------------------------------------

USE `bbdata2`;
DROP procedure IF EXISTS `clean_apikeys`;

DELIMITER $$
USE `bbdata2`$$
CREATE PROCEDURE `clean_apikeys` ()
BEGIN
	-- IMPORTANT on the server: 
	-- SET GLOBAL event_scheduler = ON;
	-- delimiter $$
	-- create event event_clean_apikeys on schedule every 1 day do begin call clean_apikeys(); end $$
	-- delimiter ;
	DELETE FROM apikeys WHERE expirationdate IS NOT NULL AND expirationdate < CURDATE();
END$$

DELIMITER ;

-- -----------------------------------------------------
-- procedure rebuild_userperms_oid
-- -----------------------------------------------------

USE `bbdata2`;
DROP procedure IF EXISTS `rebuild_userperms_oid`;

DELIMITER $$
USE `bbdata2`$$
CREATE PROCEDURE `rebuild_userperms_oid`(IN oid INT)
BEGIN
    -- same as rebuild_userperms, but delete/insert only lines with the given object
    -- this is an optimisation: objects are often created/altered so it is worth it !
    DELETE FROM userperms WHERE object_id = oid; 
    INSERT IGNORE INTO userperms 
    SELECT DISTINCT * FROM (
			-- all admin members of the ugrp owner of the object have write permission
        SELECT ug.user_id, o.id, ug.is_admin 
        FROM users_ugrps ug INNER JOIN objects o ON o.ugrp_id = ug.ugrp_id
      	WHERE o.id = oid
        UNION
        -- all members of an ugrp added to an object group having the object have read permission
        SELECT ug.user_id, og.object_id, 0
        FROM users_ugrps ug
        INNER JOIN rights r ON ug.ugrp_id = r.ugrp_id 
        INNER JOIN objects_ogrps og ON r.ogrp_id = og.ogrp_id
        INNER JOIN objects o ON og.object_id = o.id AND o.id = oid
        WHERE ug.ugrp_id <> o.ugrp_id
    ) tmp;
END$$

DELIMITER ;

-- -----------------------------------------------------
-- View `ogrps_read`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `ogrps_read`;
DROP VIEW IF EXISTS `ogrps_read` ;
USE `bbdata2`;
CREATE  OR REPLACE VIEW ogrps_read AS
SELECT DISTINCT user_id, ogrp_id
FROM (
	-- users part of the owning group (admins or regular)
	SELECT uug.user_id, og.id AS 'ogrp_id'
    FROM users_ugrps uug INNER JOIN ogrps og ON uug.ugrp_id = og.ugrp_id
    UNION
	-- users part of a userGroup having permission to the objectGroup
    SELECT uug.user_id, og.id AS 'ogrp_id'
    FROM users_ugrps uug INNER JOIN rights r ON uug.ugrp_id = r.ugrp_id INNER JOIN ogrps og ON og.id = r.ogrp_id
    -- UNION
    -- superAdmins have access to all (cross-join)
    -- SELECT uug.user_id, og.id AS 'ogrp_id' 
    -- FROM users_ugrps uug, ogrps og
    -- WHERE uug.ugrp_id = og.ugrp_id AND uug.ugrp_id = 1
) read_perms;

-- -----------------------------------------------------
-- View `ogrps_write`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `ogrps_write`;
DROP VIEW IF EXISTS `ogrps_write` ;
USE `bbdata2`;
CREATE  OR REPLACE VIEW ogrps_write AS 
SELECT DISTINCT user_id AS user_id, ogrp_id AS ogrp_id 
FROM (
	-- admins from the userGroup owning the objectGroup
    SELECT uug.user_id, o.id AS ogrp_id 
    FROM users_ugrps uug INNER JOIN ogrps o ON uug.ugrp_id = o.ugrp_id AND uug.is_admin = 1
    -- UNION 
    -- super admins have access to all (cross-join)
    -- SELECT uug.user_id AS user_id, og.id AS ogrp_id 
    -- FROM users_ugrps uug, ogrps og
    -- WHERE uug.ugrp_id = 1 AND uug.is_admin = 1
) write_perms;
USE `bbdata2`;

DELIMITER $$

USE `bbdata2`$$
DROP TRIGGER IF EXISTS `ugrps_ADEL` $$
USE `bbdata2`$$
CREATE TRIGGER `ugrps_ADEL` AFTER DELETE ON `ugrps` FOR EACH ROW
	DELETE FROM rights WHERE ugrp_id = OLD.id;$$


USE `bbdata2`$$
DROP TRIGGER IF EXISTS `objects_AINS` $$
USE `bbdata2`$$
CREATE TRIGGER `objects_AINS` AFTER INSERT ON `objects` FOR EACH ROW
	-- DAFUCK ???!!!! 
    -- INSERT IGNORE INTO objects_ogrps(ogrp_id, object_id) VALUES (NEW.ugrp_id, NEW.id);
	CALL rebuild_userperms_oid(NEW.id);$$


USE `bbdata2`$$
DROP TRIGGER IF EXISTS `objects_AUPD` $$
USE `bbdata2`$$
CREATE TRIGGER objects_AUPD AFTER UPDATE ON objects FOR EACH ROW
BEGIN
    IF NEW.disabled THEN 
      DELETE FROM tokens WHERE object_id = NEW.id;
    END IF;
END$$


USE `bbdata2`$$
DROP TRIGGER IF EXISTS `objects_ADEL` $$
USE `bbdata2`$$
CREATE TRIGGER `objects_ADEL` AFTER DELETE ON `objects` FOR EACH ROW
	CALL rebuild_userperms_oid(OLD.id);$$


USE `bbdata2`$$
DROP TRIGGER IF EXISTS `objects_ogrps_AINS` $$
USE `bbdata2`$$
CREATE TRIGGER `objects_ogrps_AINS` AFTER INSERT ON `objects_ogrps` FOR EACH ROW
	CALL rebuild_userperms_oid(NEW.object_id);$$


USE `bbdata2`$$
DROP TRIGGER IF EXISTS `objects_ogrps_ADEL` $$
USE `bbdata2`$$
CREATE TRIGGER `objects_ogrps_ADEL` AFTER DELETE ON `objects_ogrps` FOR EACH ROW
	CALL rebuild_userperms_oid(OLD.object_id);$$


USE `bbdata2`$$
DROP TRIGGER IF EXISTS `rights_AINS` $$
USE `bbdata2`$$
CREATE TRIGGER `rights_AINS` AFTER INSERT ON `rights` FOR EACH ROW
	CALL rebuild_userperms();$$


USE `bbdata2`$$
DROP TRIGGER IF EXISTS `rights_ADEL` $$
USE `bbdata2`$$
CREATE TRIGGER `rights_ADEL` AFTER DELETE ON `rights` FOR EACH ROW
	CALL rebuild_userperms();$$


USE `bbdata2`$$
DROP TRIGGER IF EXISTS `users_ugrps_AINS` $$
USE `bbdata2`$$
CREATE TRIGGER `users_ugrps_AINS` AFTER INSERT ON `users_ugrps` FOR EACH ROW
	CALL rebuild_userperms();$$


USE `bbdata2`$$
DROP TRIGGER IF EXISTS `users_ugrps_ADEL` $$
USE `bbdata2`$$
CREATE TRIGGER `users_ugrps_ADEL` AFTER DELETE ON `users_ugrps` FOR EACH ROW
	CALL rebuild_userperms();$$


DELIMITER ;

SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
