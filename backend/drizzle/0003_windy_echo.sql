ALTER TABLE `devices` ADD `device_id` text;--> statement-breakpoint
UPDATE `devices` SET `device_id` = `android_id` WHERE `device_id` IS NULL;--> statement-breakpoint
CREATE INDEX `idx_devices_device_id` ON `devices` (`device_id`);
