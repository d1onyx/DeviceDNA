CREATE TABLE `user_devices` (
	`user_uid` text NOT NULL,
	`device_id` text NOT NULL,
	`created_at` integer NOT NULL,
	PRIMARY KEY(`user_uid`, `device_id`),
	FOREIGN KEY (`user_uid`) REFERENCES `users`(`firebase_uid`) ON UPDATE no action ON DELETE cascade,
	FOREIGN KEY (`device_id`) REFERENCES `devices`(`id`) ON UPDATE no action ON DELETE cascade
);
--> statement-breakpoint
CREATE INDEX `idx_user_devices_user` ON `user_devices` (`user_uid`);--> statement-breakpoint
CREATE INDEX `idx_user_devices_device` ON `user_devices` (`device_id`);--> statement-breakpoint
INSERT INTO `user_devices` (`user_uid`, `device_id`, `created_at`)
SELECT `user_uid`, `id`, `created_at` FROM `devices`;--> statement-breakpoint
UPDATE `devices` SET `device_id` = `android_id` WHERE `device_id` IS NULL;--> statement-breakpoint
DROP INDEX `uq_user_android`;--> statement-breakpoint
DROP INDEX `idx_devices_device_id`;--> statement-breakpoint
CREATE UNIQUE INDEX `uq_devices_device_id` ON `devices` (`device_id`);--> statement-breakpoint
CREATE UNIQUE INDEX `uq_devices_android_id` ON `devices` (`android_id`);
