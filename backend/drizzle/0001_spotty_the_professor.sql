CREATE TABLE `google_play_purchase_owners` (
	`purchase_token` text PRIMARY KEY NOT NULL,
	`user_uid` text NOT NULL,
	`created_at` integer NOT NULL,
	FOREIGN KEY (`user_uid`) REFERENCES `users`(`firebase_uid`) ON UPDATE no action ON DELETE cascade
);
--> statement-breakpoint
CREATE INDEX `idx_google_play_purchase_owners_user` ON `google_play_purchase_owners` (`user_uid`);--> statement-breakpoint
INSERT OR IGNORE INTO `google_play_purchase_owners` (`purchase_token`, `user_uid`, `created_at`)
SELECT `latest_purchase_token`, `user_uid`, `created_at`
FROM `user_subscriptions`
WHERE `latest_purchase_token` IS NOT NULL;--> statement-breakpoint
CREATE UNIQUE INDEX `uq_user_subscriptions_purchase_token` ON `user_subscriptions` (`latest_purchase_token`);
