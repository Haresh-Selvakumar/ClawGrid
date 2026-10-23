package com.example.data.remote

/** Cloud writes are intentionally unavailable until user auth, membership and RLS are integrated. */
object SupabaseClient {
    const val RECOMMENDED_SUPABASE_SQL = "-- No deployment SQL in Android. Inspect and verify authenticated home-scoped migrations server-side."
}
