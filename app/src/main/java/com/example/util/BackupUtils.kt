package com.example.util

import com.example.data.model.CharacterRelationship
import com.example.data.model.FeedPost
import com.example.data.model.LoreCharacter
import com.example.data.model.LoreEntry
import com.example.data.model.LyricEntry
import com.example.data.model.MemberProfile
import com.example.data.model.PhotoAlbum
import com.example.data.model.PhotoItem
import com.example.data.model.QuoteEntry
import org.json.JSONArray
import org.json.JSONObject

enum class BackupSection(val label: String) {
    FULL_COMMUNITY("Full Community Backup (No Private Chats)"),
    PERSONAL_PROFILE("Personal Profile"),
    LORE_BOOK("Lore Book (Characters, Lore & Relationships)"),
    QUOTES("Quote Book"),
    LYRICS("Song Lyrics Library"),
    PHOTO_ALBUMS("Photo Albums & Media"),
    SOCIAL_FEED("Social Feed Posts")
}

object BackupUtils {
    fun exportJson(
        section: BackupSection,
        myProfile: MemberProfile?,
        posts: List<FeedPost>,
        characters: List<LoreCharacter>,
        loreEntries: List<LoreEntry>,
        relationships: List<CharacterRelationship>,
        quotes: List<QuoteEntry>,
        lyrics: List<LyricEntry>,
        albums: List<PhotoAlbum>,
        photos: List<PhotoItem>
    ): String {
        val root = JSONObject()
        root.put("format", "social-lore-backup-v1")
        root.put("section", section.name)
        root.put("exportedAtMillis", System.currentTimeMillis())
        root.put("privateMessagesExcluded", true)

        if (section == BackupSection.PERSONAL_PROFILE || section == BackupSection.FULL_COMMUNITY) {
            myProfile?.let { p ->
                root.put("profile", JSONObject().apply {
                    put("displayName", p.displayName)
                    put("tagline", p.tagline ?: "")
                    put("bio", p.bio ?: "")
                    put("avatarUrl", p.avatarUrl ?: "")
                    put("bannerUrl", p.bannerUrl ?: "")
                    put("wallpaperUrl", p.wallpaperUrl ?: "")
                    put("profileMusicTitle", p.profileMusicTitle ?: "")
                    put("profileMusicArtist", p.profileMusicArtist ?: "")
                    put("profileMusicUrl", p.profileMusicUrl ?: "")
                    put("layoutStyle", p.layoutStyle)
                    put("themeId", p.themeId)
                    put("headingFont", p.headingFont ?: "")
                    put("bodyFont", p.bodyFont ?: "")
                })
            }
        }

        if (section == BackupSection.LORE_BOOK || section == BackupSection.FULL_COMMUNITY) {
            root.put("characters", JSONArray().apply {
                characters.forEach { c ->
                    put(JSONObject().apply {
                        put("name", c.name)
                        put("nicknames", JSONArray(c.nicknames))
                        put("aliases", JSONArray(c.aliases))
                        put("portraitUrl", c.portraitUrl ?: "")
                        put("biography", c.biography)
                        put("customDetails", JSONObject(c.customDetails))
                    })
                }
            })
            root.put("loreEntries", JSONArray().apply {
                loreEntries.forEach { l ->
                    put(JSONObject().apply {
                        put("characterName", l.characterName)
                        put("title", l.title)
                        put("story", l.story)
                        put("datePrecision", l.datePrecision)
                        put("historicalDateLabel", l.historicalDateLabel)
                        put("sortOrderEpoch", l.sortOrderEpoch)
                    })
                }
            })
            root.put("relationships", JSONArray().apply {
                relationships.forEach { r ->
                    put(JSONObject().apply {
                        put("fromCharacterName", r.fromCharacterName)
                        put("toCharacterName", r.toCharacterName)
                        put("label", r.label)
                        put("category", r.category)
                        put("notes", r.notes ?: "")
                    })
                }
            })
        }

        if (section == BackupSection.QUOTES || section == BackupSection.FULL_COMMUNITY) {
            root.put("quotes", JSONArray().apply {
                quotes.forEach { q ->
                    put(JSONObject().apply {
                        put("text", q.text)
                        put("attributedToName", q.attributedToName)
                        put("attributedType", q.attributedType)
                        put("dateLabel", q.dateLabel ?: "")
                        put("context", q.context ?: "")
                        put("category", q.category)
                    })
                }
            })
        }

        if (section == BackupSection.LYRICS || section == BackupSection.FULL_COMMUNITY) {
            root.put("lyrics", JSONArray().apply {
                lyrics.forEach { ly ->
                    put(JSONObject().apply {
                        put("excerpt", ly.excerpt)
                        put("songTitle", ly.songTitle)
                        put("artist", ly.artist)
                        put("commentary", ly.commentary ?: "")
                        put("songLink", ly.songLink ?: "")
                        put("category", ly.category)
                    })
                }
            })
        }

        if (section == BackupSection.PHOTO_ALBUMS || section == BackupSection.FULL_COMMUNITY) {
            root.put("albums", JSONArray().apply {
                albums.forEach { a ->
                    put(JSONObject().apply {
                        put("title", a.title)
                        put("description", a.description ?: "")
                        put("coverImageUrl", a.coverImageUrl ?: "")
                    })
                }
            })
            root.put("photos", JSONArray().apply {
                photos.forEach { ph ->
                    put(JSONObject().apply {
                        put("albumTitle", ph.albumTitle)
                        put("imageUrl", ph.imageUrl)
                        put("caption", ph.caption ?: "")
                        put("dateTakenLabel", ph.dateTakenLabel ?: "")
                        put("taggedCharacterNames", JSONArray(ph.taggedCharacterNames))
                    })
                }
            })
        }

        if (section == BackupSection.SOCIAL_FEED || section == BackupSection.FULL_COMMUNITY) {
            root.put("posts", JSONArray().apply {
                posts.filter { it.postType == "TEXT" || it.postType == "PHOTO" }.forEach { p ->
                    put(JSONObject().apply {
                        put("postType", p.postType)
                        put("text", p.text)
                        put("imageUrl", p.imageUrl ?: "")
                    })
                }
            })
        }

        return root.toString(2)
    }
}
