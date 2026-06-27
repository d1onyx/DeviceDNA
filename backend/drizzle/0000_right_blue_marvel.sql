CREATE TABLE "devices" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"user_uid" text NOT NULL,
	"android_id" text NOT NULL,
	"device_name" text,
	"manufacturer" text,
	"model" text,
	"os_version" text,
	"app_version" text,
	"snapshot" jsonb,
	"snapshot_hash" text,
	"last_synced_at" timestamp with time zone DEFAULT now() NOT NULL,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL,
	CONSTRAINT "uq_user_android" UNIQUE("user_uid","android_id")
);
--> statement-breakpoint
CREATE TABLE "users" (
	"firebase_uid" text PRIMARY KEY NOT NULL,
	"email" text,
	"display_name" text,
	"photo_url" text,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL,
	"updated_at" timestamp with time zone DEFAULT now() NOT NULL
);
--> statement-breakpoint
ALTER TABLE "devices" ADD CONSTRAINT "devices_user_uid_users_firebase_uid_fk" FOREIGN KEY ("user_uid") REFERENCES "public"."users"("firebase_uid") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
CREATE INDEX "idx_devices_user" ON "devices" USING btree ("user_uid");