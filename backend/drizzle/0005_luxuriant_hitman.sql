ALTER TABLE `device_info` RENAME COLUMN "android_id" TO "platform_device_id";--> statement-breakpoint
ALTER TABLE `user_devices` RENAME COLUMN `device_id` TO `device_row_id`;--> statement-breakpoint
DROP INDEX `uq_devices_android_id`;--> statement-breakpoint
ALTER TABLE `devices` DROP COLUMN `android_id`;
