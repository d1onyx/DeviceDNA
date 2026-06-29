CREATE TABLE "user_subscriptions" (
	"user_uid" text PRIMARY KEY NOT NULL,
	"status" text DEFAULT 'inactive' NOT NULL,
	"provider" text DEFAULT 'manual' NOT NULL,
	"product_id" text,
	"original_transaction_id" text,
	"latest_transaction_id" text,
	"expires_at" timestamp with time zone,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL,
	"updated_at" timestamp with time zone DEFAULT now() NOT NULL
);
--> statement-breakpoint
ALTER TABLE "user_subscriptions" ADD CONSTRAINT "user_subscriptions_user_uid_users_firebase_uid_fk" FOREIGN KEY ("user_uid") REFERENCES "public"."users"("firebase_uid") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
CREATE INDEX "idx_user_subscriptions_provider" ON "user_subscriptions" USING btree ("provider");--> statement-breakpoint
CREATE INDEX "idx_user_subscriptions_expires_at" ON "user_subscriptions" USING btree ("expires_at");