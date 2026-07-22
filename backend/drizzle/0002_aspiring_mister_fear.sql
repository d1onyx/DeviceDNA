CREATE TABLE `app_store_transaction_owners` (
	`original_transaction_id` text PRIMARY KEY NOT NULL,
	`user_uid` text NOT NULL,
	`created_at` integer NOT NULL,
	FOREIGN KEY (`user_uid`) REFERENCES `users`(`firebase_uid`) ON UPDATE no action ON DELETE cascade
);
--> statement-breakpoint
CREATE INDEX `idx_app_store_transaction_owners_user` ON `app_store_transaction_owners` (`user_uid`);