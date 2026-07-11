CREATE TABLE `app_list_info` (
	`device_id` text PRIMARY KEY NOT NULL,
	`total_count` integer NOT NULL,
	`user_count` integer NOT NULL,
	`system_count` integer NOT NULL,
	FOREIGN KEY (`device_id`) REFERENCES `devices`(`id`) ON UPDATE no action ON DELETE cascade
);
--> statement-breakpoint
CREATE TABLE `apps` (
	`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	`device_id` text NOT NULL,
	`name` text NOT NULL,
	`package_name` text NOT NULL,
	`version_name` text NOT NULL,
	`version_code` integer NOT NULL,
	`is_system_app` integer NOT NULL,
	`installed_at` integer,
	`updated_at` integer,
	`target_sdk_version` integer NOT NULL,
	FOREIGN KEY (`device_id`) REFERENCES `devices`(`id`) ON UPDATE no action ON DELETE cascade
);
--> statement-breakpoint
CREATE INDEX `idx_apps_device` ON `apps` (`device_id`);--> statement-breakpoint
CREATE TABLE `battery_info` (
	`device_id` text PRIMARY KEY NOT NULL,
	`level_percent` integer NOT NULL,
	`status` text NOT NULL,
	`health` text NOT NULL,
	`source` text NOT NULL,
	`technology` text NOT NULL,
	`temperature_celsius` real NOT NULL,
	`voltage_mv` integer NOT NULL,
	`current_ma` integer,
	`capacity_mah` integer,
	`charge_cycles` integer,
	`is_present` integer NOT NULL,
	`estimated_watts` real,
	`charge_time_remaining_ms` integer,
	`is_power_save_mode` integer DEFAULT false NOT NULL,
	FOREIGN KEY (`device_id`) REFERENCES `devices`(`id`) ON UPDATE no action ON DELETE cascade
);
--> statement-breakpoint
CREATE TABLE `camera_apertures` (
	`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	`camera_row_id` integer NOT NULL,
	`value` real NOT NULL,
	FOREIGN KEY (`camera_row_id`) REFERENCES `cameras`(`id`) ON UPDATE no action ON DELETE cascade
);
--> statement-breakpoint
CREATE INDEX `idx_camera_apertures_camera` ON `camera_apertures` (`camera_row_id`);--> statement-breakpoint
CREATE TABLE `camera_focal_lengths` (
	`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	`camera_row_id` integer NOT NULL,
	`value` real NOT NULL,
	FOREIGN KEY (`camera_row_id`) REFERENCES `cameras`(`id`) ON UPDATE no action ON DELETE cascade
);
--> statement-breakpoint
CREATE INDEX `idx_camera_focal_lengths_camera` ON `camera_focal_lengths` (`camera_row_id`);--> statement-breakpoint
CREATE TABLE `camera_supported_modes` (
	`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	`camera_row_id` integer NOT NULL,
	`value` text NOT NULL,
	FOREIGN KEY (`camera_row_id`) REFERENCES `cameras`(`id`) ON UPDATE no action ON DELETE cascade
);
--> statement-breakpoint
CREATE INDEX `idx_camera_supported_modes_camera` ON `camera_supported_modes` (`camera_row_id`);--> statement-breakpoint
CREATE TABLE `cameras` (
	`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	`device_id` text NOT NULL,
	`camera_id` text NOT NULL,
	`facing` text NOT NULL,
	`megapixels` real NOT NULL,
	`resolution_width` integer NOT NULL,
	`resolution_height` integer NOT NULL,
	`has_flash` integer NOT NULL,
	`has_ois` integer NOT NULL,
	`max_video_width` integer DEFAULT 0 NOT NULL,
	`max_video_height` integer DEFAULT 0 NOT NULL,
	`max_video_fps` integer DEFAULT 0 NOT NULL,
	`max_slow_mo_fps` integer DEFAULT 0 NOT NULL,
	`slow_mo_width` integer DEFAULT 0 NOT NULL,
	`slow_mo_height` integer DEFAULT 0 NOT NULL,
	`max_photo_width` integer DEFAULT 0 NOT NULL,
	`max_photo_height` integer DEFAULT 0 NOT NULL,
	`min_exposure_nanos` integer DEFAULT 0 NOT NULL,
	`max_exposure_nanos` integer DEFAULT 0 NOT NULL,
	FOREIGN KEY (`device_id`) REFERENCES `devices`(`id`) ON UPDATE no action ON DELETE cascade
);
--> statement-breakpoint
CREATE INDEX `idx_cameras_device` ON `cameras` (`device_id`);--> statement-breakpoint
CREATE TABLE `connectivity_info` (
	`device_id` text PRIMARY KEY NOT NULL,
	`has_wifi` integer NOT NULL,
	`has_wifi_5ghz` integer NOT NULL,
	`has_wifi_6ghz` integer NOT NULL,
	`has_wifi_direct` integer NOT NULL,
	`has_bluetooth` integer NOT NULL,
	`has_bluetooth_le` integer NOT NULL,
	`has_nfc` integer NOT NULL,
	`has_uwb` integer NOT NULL,
	`has_esim` integer NOT NULL,
	`bluetooth_version` text,
	FOREIGN KEY (`device_id`) REFERENCES `devices`(`id`) ON UPDATE no action ON DELETE cascade
);
--> statement-breakpoint
CREATE TABLE `connectivity_wifi_standards` (
	`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	`device_id` text NOT NULL,
	`value` text NOT NULL,
	FOREIGN KEY (`device_id`) REFERENCES `devices`(`id`) ON UPDATE no action ON DELETE cascade
);
--> statement-breakpoint
CREATE INDEX `idx_connectivity_wifi_standards_device` ON `connectivity_wifi_standards` (`device_id`);--> statement-breakpoint
CREATE TABLE `cpu_cluster_core_indices` (
	`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	`cluster_id` integer NOT NULL,
	`core_index` integer NOT NULL,
	FOREIGN KEY (`cluster_id`) REFERENCES `cpu_clusters`(`id`) ON UPDATE no action ON DELETE cascade
);
--> statement-breakpoint
CREATE INDEX `idx_cpu_cluster_core_indices_cluster` ON `cpu_cluster_core_indices` (`cluster_id`);--> statement-breakpoint
CREATE TABLE `cpu_clusters` (
	`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	`device_id` text NOT NULL,
	`name` text NOT NULL,
	`min_frequency_mhz` integer NOT NULL,
	`max_frequency_mhz` integer NOT NULL,
	FOREIGN KEY (`device_id`) REFERENCES `devices`(`id`) ON UPDATE no action ON DELETE cascade
);
--> statement-breakpoint
CREATE INDEX `idx_cpu_clusters_device` ON `cpu_clusters` (`device_id`);--> statement-breakpoint
CREATE TABLE `cpu_cores` (
	`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	`device_id` text NOT NULL,
	`core_index` integer NOT NULL,
	`current_frequency_khz` integer,
	`min_frequency_khz` integer NOT NULL,
	`max_frequency_khz` integer NOT NULL,
	`is_online` integer NOT NULL,
	FOREIGN KEY (`device_id`) REFERENCES `devices`(`id`) ON UPDATE no action ON DELETE cascade
);
--> statement-breakpoint
CREATE INDEX `idx_cpu_cores_device` ON `cpu_cores` (`device_id`);--> statement-breakpoint
CREATE TABLE `cpu_info` (
	`device_id` text PRIMARY KEY NOT NULL,
	`chipset_name` text NOT NULL,
	`architecture` text NOT NULL,
	`core_count` integer NOT NULL,
	`governor` text NOT NULL,
	`temperature_celsius` real,
	`usage_percent` real,
	`process_count` integer,
	`min_freq_mhz` integer DEFAULT 0 NOT NULL,
	`max_freq_mhz` integer DEFAULT 0 NOT NULL,
	`gpu_renderer` text NOT NULL,
	`gpu_vendor` text NOT NULL,
	`gpu_version` text NOT NULL,
	FOREIGN KEY (`device_id`) REFERENCES `devices`(`id`) ON UPDATE no action ON DELETE cascade
);
--> statement-breakpoint
CREATE TABLE `cpu_instruction_sets` (
	`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	`device_id` text NOT NULL,
	`value` text NOT NULL,
	FOREIGN KEY (`device_id`) REFERENCES `devices`(`id`) ON UPDATE no action ON DELETE cascade
);
--> statement-breakpoint
CREATE INDEX `idx_cpu_instruction_sets_device` ON `cpu_instruction_sets` (`device_id`);--> statement-breakpoint
CREATE TABLE `device_info` (
	`device_id` text PRIMARY KEY NOT NULL,
	`name` text NOT NULL,
	`model` text NOT NULL,
	`manufacturer` text NOT NULL,
	`brand` text NOT NULL,
	`board` text NOT NULL,
	`hardware` text NOT NULL,
	`codename` text NOT NULL,
	`build_fingerprint` text NOT NULL,
	`android_id` text NOT NULL,
	`is_rooted` integer NOT NULL,
	`bootloader` text NOT NULL,
	`soc_name` text DEFAULT '' NOT NULL,
	`serial_number` text DEFAULT '' NOT NULL,
	`is_emulator` integer DEFAULT false NOT NULL,
	`is_developer_options_enabled` integer DEFAULT false NOT NULL,
	`is_adb_enabled` integer DEFAULT false NOT NULL,
	`build_tags` text DEFAULT '' NOT NULL,
	`build_user` text DEFAULT '' NOT NULL,
	`build_host` text DEFAULT '' NOT NULL,
	`build_time_millis` integer DEFAULT 0 NOT NULL,
	`is_test_keys_build` integer DEFAULT false NOT NULL,
	`is_debuggable_build` integer DEFAULT false NOT NULL,
	`verified_boot_state` text DEFAULT '' NOT NULL,
	`boot_verified_state` text DEFAULT '' NOT NULL,
	`vb_meta_device_state` text DEFAULT '' NOT NULL,
	`flash_locked` text DEFAULT '' NOT NULL,
	`verity_mode` text DEFAULT '' NOT NULL,
	`warranty_bit` text DEFAULT '' NOT NULL,
	`first_api_level` integer,
	FOREIGN KEY (`device_id`) REFERENCES `devices`(`id`) ON UPDATE no action ON DELETE cascade
);
--> statement-breakpoint
CREATE TABLE `device_supported_abis` (
	`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	`device_id` text NOT NULL,
	`value` text NOT NULL,
	FOREIGN KEY (`device_id`) REFERENCES `devices`(`id`) ON UPDATE no action ON DELETE cascade
);
--> statement-breakpoint
CREATE INDEX `idx_device_supported_abis_device` ON `device_supported_abis` (`device_id`);--> statement-breakpoint
CREATE TABLE `device_suspicious_root_paths` (
	`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	`device_id` text NOT NULL,
	`value` text NOT NULL,
	FOREIGN KEY (`device_id`) REFERENCES `devices`(`id`) ON UPDATE no action ON DELETE cascade
);
--> statement-breakpoint
CREATE INDEX `idx_device_suspicious_root_paths_device` ON `device_suspicious_root_paths` (`device_id`);--> statement-breakpoint
CREATE TABLE `devices` (
	`id` text PRIMARY KEY NOT NULL,
	`user_uid` text NOT NULL,
	`android_id` text NOT NULL,
	`device_name` text,
	`manufacturer` text,
	`model` text,
	`os_version` text,
	`app_version` text,
	`snapshot_hash` text,
	`last_synced_at` integer NOT NULL,
	`created_at` integer NOT NULL,
	FOREIGN KEY (`user_uid`) REFERENCES `users`(`firebase_uid`) ON UPDATE no action ON DELETE cascade
);
--> statement-breakpoint
CREATE UNIQUE INDEX `uq_user_android` ON `devices` (`user_uid`,`android_id`);--> statement-breakpoint
CREATE INDEX `idx_devices_user` ON `devices` (`user_uid`);--> statement-breakpoint
CREATE TABLE `display_hdr_capabilities` (
	`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	`device_id` text NOT NULL,
	`value` text NOT NULL,
	FOREIGN KEY (`device_id`) REFERENCES `devices`(`id`) ON UPDATE no action ON DELETE cascade
);
--> statement-breakpoint
CREATE INDEX `idx_display_hdr_capabilities_device` ON `display_hdr_capabilities` (`device_id`);--> statement-breakpoint
CREATE TABLE `display_info` (
	`device_id` text PRIMARY KEY NOT NULL,
	`width_px` integer NOT NULL,
	`height_px` integer NOT NULL,
	`density_dpi` integer NOT NULL,
	`density_bucket` text NOT NULL,
	`font_scale` real NOT NULL,
	`physical_size_inches` real NOT NULL,
	`refresh_rate_hz` real NOT NULL,
	`is_hdr` integer NOT NULL,
	`is_wide_color_gamut` integer NOT NULL,
	`brightness_level` real NOT NULL,
	`is_adaptive_brightness` integer NOT NULL,
	`orientation` text NOT NULL,
	`display_type` text NOT NULL,
	FOREIGN KEY (`device_id`) REFERENCES `devices`(`id`) ON UPDATE no action ON DELETE cascade
);
--> statement-breakpoint
CREATE TABLE `display_supported_refresh_rates` (
	`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	`device_id` text NOT NULL,
	`value` real NOT NULL,
	FOREIGN KEY (`device_id`) REFERENCES `devices`(`id`) ON UPDATE no action ON DELETE cascade
);
--> statement-breakpoint
CREATE INDEX `idx_display_supported_refresh_rates_device` ON `display_supported_refresh_rates` (`device_id`);--> statement-breakpoint
CREATE TABLE `fraud_signals` (
	`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	`device_id` text NOT NULL,
	`signal_id` text NOT NULL,
	`label` text NOT NULL,
	`severity` text NOT NULL,
	`evidence` text NOT NULL,
	FOREIGN KEY (`device_id`) REFERENCES `devices`(`id`) ON UPDATE no action ON DELETE cascade
);
--> statement-breakpoint
CREATE INDEX `idx_fraud_signals_device` ON `fraud_signals` (`device_id`);--> statement-breakpoint
CREATE TABLE `health_insight_actions` (
	`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	`insight_row_id` integer NOT NULL,
	`label` text NOT NULL,
	`description` text NOT NULL,
	FOREIGN KEY (`insight_row_id`) REFERENCES `health_insights`(`id`) ON UPDATE no action ON DELETE cascade
);
--> statement-breakpoint
CREATE INDEX `idx_health_insight_actions_insight` ON `health_insight_actions` (`insight_row_id`);--> statement-breakpoint
CREATE TABLE `health_insights` (
	`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	`device_id` text NOT NULL,
	`insight_id` text NOT NULL,
	`title` text NOT NULL,
	`summary` text NOT NULL,
	`severity` text NOT NULL,
	`confidence` real NOT NULL,
	FOREIGN KEY (`device_id`) REFERENCES `devices`(`id`) ON UPDATE no action ON DELETE cascade
);
--> statement-breakpoint
CREATE INDEX `idx_health_insights_device` ON `health_insights` (`device_id`);--> statement-breakpoint
CREATE TABLE `health_score` (
	`device_id` text PRIMARY KEY NOT NULL,
	`overall` integer NOT NULL,
	`battery` integer NOT NULL,
	`performance` integer NOT NULL,
	`storage` integer NOT NULL,
	`security` integer NOT NULL,
	`thermal` integer NOT NULL,
	`fraud_score` integer DEFAULT 0 NOT NULL,
	`fraud_level` text DEFAULT 'Low' NOT NULL,
	FOREIGN KEY (`device_id`) REFERENCES `devices`(`id`) ON UPDATE no action ON DELETE cascade
);
--> statement-breakpoint
CREATE TABLE `network_active_transports` (
	`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	`device_id` text NOT NULL,
	`value` text NOT NULL,
	FOREIGN KEY (`device_id`) REFERENCES `devices`(`id`) ON UPDATE no action ON DELETE cascade
);
--> statement-breakpoint
CREATE INDEX `idx_network_active_transports_device` ON `network_active_transports` (`device_id`);--> statement-breakpoint
CREATE TABLE `network_dns` (
	`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	`device_id` text NOT NULL,
	`value` text NOT NULL,
	FOREIGN KEY (`device_id`) REFERENCES `devices`(`id`) ON UPDATE no action ON DELETE cascade
);
--> statement-breakpoint
CREATE INDEX `idx_network_dns_device` ON `network_dns` (`device_id`);--> statement-breakpoint
CREATE TABLE `network_info` (
	`device_id` text PRIMARY KEY NOT NULL,
	`connection_type` text NOT NULL,
	`ssid` text,
	`local_ipv4` text,
	`local_ipv6` text,
	`gateway` text,
	`subnet_mask` text,
	`interface_name` text,
	`link_speed_mbps` integer,
	`frequency_mhz` integer,
	`channel` integer,
	`wifi_standard` text,
	`security_type` text,
	`signal_strength` integer,
	`mac_address` text,
	`rx_bytes_per_sec` integer,
	`tx_bytes_per_sec` integer,
	`is_metered` integer DEFAULT false NOT NULL,
	`cellular_operator` text,
	`cellular_generation` text,
	`is_vpn_active` integer DEFAULT false NOT NULL,
	`is_validated_internet` integer DEFAULT false NOT NULL,
	`is_captive_portal` integer DEFAULT false NOT NULL,
	`private_dns_server_name` text,
	`http_proxy_host` text,
	`http_proxy_port` integer,
	FOREIGN KEY (`device_id`) REFERENCES `devices`(`id`) ON UPDATE no action ON DELETE cascade
);
--> statement-breakpoint
CREATE TABLE `ram_info` (
	`device_id` text PRIMARY KEY NOT NULL,
	`total_bytes` integer NOT NULL,
	`available_bytes` integer NOT NULL,
	`used_bytes` integer NOT NULL,
	`used_percent` real NOT NULL,
	`is_low_memory` integer NOT NULL,
	`cached_bytes` integer DEFAULT 0 NOT NULL,
	`threshold_bytes` integer DEFAULT 0 NOT NULL,
	FOREIGN KEY (`device_id`) REFERENCES `devices`(`id`) ON UPDATE no action ON DELETE cascade
);
--> statement-breakpoint
CREATE TABLE `sensors` (
	`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	`device_id` text NOT NULL,
	`name` text NOT NULL,
	`vendor` text NOT NULL,
	`type` integer NOT NULL,
	`type_name` text NOT NULL,
	`version` integer NOT NULL,
	`power_ma` real NOT NULL,
	`resolution` real NOT NULL,
	`max_range` real NOT NULL,
	`is_wake_up` integer NOT NULL,
	`is_dynamic` integer NOT NULL,
	FOREIGN KEY (`device_id`) REFERENCES `devices`(`id`) ON UPDATE no action ON DELETE cascade
);
--> statement-breakpoint
CREATE INDEX `idx_sensors_device` ON `sensors` (`device_id`);--> statement-breakpoint
CREATE TABLE `storage_info` (
	`device_id` text PRIMARY KEY NOT NULL,
	`total_bytes` integer NOT NULL,
	`used_bytes` integer NOT NULL,
	`free_bytes` integer NOT NULL,
	`used_percent` real NOT NULL,
	`external_total_bytes` integer DEFAULT 0 NOT NULL,
	`external_free_bytes` integer DEFAULT 0 NOT NULL,
	FOREIGN KEY (`device_id`) REFERENCES `devices`(`id`) ON UPDATE no action ON DELETE cascade
);
--> statement-breakpoint
CREATE TABLE `system_info` (
	`device_id` text PRIMARY KEY NOT NULL,
	`android_version` text NOT NULL,
	`api_level` integer NOT NULL,
	`security_patch_level` text NOT NULL,
	`build_number` text NOT NULL,
	`kernel_version` text NOT NULL,
	`java_vm` text NOT NULL,
	`open_gl_version` text NOT NULL,
	`baseband` text NOT NULL,
	`bootloader` text NOT NULL,
	`language` text NOT NULL,
	`time_zone` text NOT NULL,
	`release_name` text NOT NULL,
	`uptime_millis` integer DEFAULT 0 NOT NULL,
	`build_type` text DEFAULT '' NOT NULL,
	`running_process_count` integer DEFAULT 0 NOT NULL,
	`se_linux_status` text DEFAULT '' NOT NULL,
	`is_encrypted` integer DEFAULT true NOT NULL,
	`gl_es_version` text DEFAULT '' NOT NULL,
	`total_ram_gb` real DEFAULT 0 NOT NULL,
	`is_app_debuggable` integer DEFAULT false NOT NULL,
	`installer_package_name` text,
	`signing_certificate_sha256` text,
	`app_version_name` text DEFAULT '' NOT NULL,
	`app_version_code` integer DEFAULT 0 NOT NULL,
	`package_name` text DEFAULT '' NOT NULL,
	`is_installed_from_known_store` integer DEFAULT false NOT NULL,
	`is_power_save_mode` integer DEFAULT false NOT NULL,
	FOREIGN KEY (`device_id`) REFERENCES `devices`(`id`) ON UPDATE no action ON DELETE cascade
);
--> statement-breakpoint
CREATE TABLE `thermal_zones` (
	`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	`device_id` text NOT NULL,
	`name` text NOT NULL,
	`type` text NOT NULL,
	`temperature_celsius` real,
	FOREIGN KEY (`device_id`) REFERENCES `devices`(`id`) ON UPDATE no action ON DELETE cascade
);
--> statement-breakpoint
CREATE INDEX `idx_thermal_zones_device` ON `thermal_zones` (`device_id`);--> statement-breakpoint
CREATE TABLE `user_subscriptions` (
	`user_uid` text PRIMARY KEY NOT NULL,
	`status` text DEFAULT 'inactive' NOT NULL,
	`provider` text DEFAULT 'manual' NOT NULL,
	`product_id` text,
	`original_transaction_id` text,
	`latest_transaction_id` text,
	`latest_purchase_token` text,
	`expires_at` integer,
	`created_at` integer NOT NULL,
	`updated_at` integer NOT NULL,
	FOREIGN KEY (`user_uid`) REFERENCES `users`(`firebase_uid`) ON UPDATE no action ON DELETE cascade
);
--> statement-breakpoint
CREATE INDEX `idx_user_subscriptions_provider` ON `user_subscriptions` (`provider`);--> statement-breakpoint
CREATE INDEX `idx_user_subscriptions_expires_at` ON `user_subscriptions` (`expires_at`);--> statement-breakpoint
CREATE TABLE `users` (
	`firebase_uid` text PRIMARY KEY NOT NULL,
	`email` text,
	`display_name` text,
	`photo_url` text,
	`created_at` integer NOT NULL,
	`updated_at` integer NOT NULL
);
