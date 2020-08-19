START TRANSACTION;

INSERT INTO `types` VALUES ('string'), ('float'), ('bool'), ('int');

INSERT INTO `units` VALUES 
    (' ','none','string'),
    ('%','percentage','float'),
    ('°C','degree celsius C','float'),
    ('char','free text','string'),
    ('h','hours','int'),
    ('Here','presence','bool'),
    ('Hz','frequence','float'),
    ('kW','power','float'),
    ('lx', 'lux', 'float'),
    ('V','volt','float'),
    ('W', 'watts', 'float');


INSERT INTO `ugrps` (`id`, `name`) VALUES 
    (1, 'admin'),
    (2, 'bb'),
    (3, 'aa');


INSERT INTO `users` (`id`, `name`, `email`, `password`) VALUES 
    (1, 'admin', 'lucy@lala.com', MD5('testtest')),
    (2, 'bbb', 'bb@lala.com', MD5('testtest'));


INSERT INTO `users_ugrps` (`user_id`, `ugrp_id`, `is_admin`) VALUES 
    (1, 1, 1),
    (1, 2, 1),
    (2, 2, 0);


INSERT INTO `ogrps` (`id`, `name`, `ugrp_id`) VALUES 
    (1, 'misc', 1),
    (2, 'temp', 1),
    (3, 'aa objects', 3);


INSERT INTO `objects` (`id`, `name`, `description`, `ugrp_id`, `unit_symbol`, `creationdate`) VALUES
    (1, 'volts box 1', '', 1, 'V', '2019-01-01'),
    (2, 'volts box 2', NULL, 1, 'V', '2019-01-01'),
    (3, 'tmp box 1', NULL, 1, 'h', '2019-01-01'),
    (4, 'tmp box 2', NULL, 1, 'Here', DEFAULT),
    (5, 'caching', NULL, 1, 'V', DEFAULT),
    (6, 'owned by aa', NULL, 3, 'V', DEFAULT),
    (3008, 'blueFactory sensor', 'test values endpoint', 1, 'W', DEFAULT),
    (6602, 'aggr simple', 'test for aggr simple', 1, 'lx', '2019-01-01'),
    (13370, 'aggr extended', 'test for agg extended', 1, 'V', '2019-01-01');


INSERT INTO `objects_ogrps` (`ogrp_id`, `object_id`) VALUES
    -- misc has volts box 1 & 2
    (1, 1),
    (1, 2),
    -- tmp has tmp box 1 & 2
    (2, 3),
    (2, 4),
    -- owned by aa in group aa objects
    (3, 6);


INSERT INTO `apikeys`(`id`, `user_id`, `secret`, `readonly`) VALUES
    (1, 1, 'wr1', 0),
    (2, 1, 'ro1', 1),
    (3, 2, 'wr2', 0),
    (4, 2, 'ro2', 1);


INSERT INTO `rights` (`ogrp_id`, `ugrp_id`) VALUES 
    (1, 2);

INSERT INTO `comments` (`object_id`, `dfrom`, `dto`, `comment`) VALUES 
    (1, '2019-01-01T10:00', '2020-01-01T10:00', 'comment on one full year'),
    (1, '2019-12-31T20:00', '2020-01-01T02:00', 'happy new year !');

INSERT INTO `tokens` (`id`, `token`, `object_id`, `description`) VALUES
    (1, '012345678901234567890123456789a1', 1, 'test'),
    (2, '012345678901234567890123456789a2', 2, 'test'),
    (3, '012345678901234567890123456789a3', 3, 'test'),
    (4, '012345678901234567890123456789a4', 4, 'test');

COMMIT;
