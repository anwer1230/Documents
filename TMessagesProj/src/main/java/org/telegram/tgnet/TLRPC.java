/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.tgnet;

import android.graphics.Bitmap;
import android.graphics.Path;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.ImageLoader;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.SvgHelper;
import org.telegram.messenger.TranslateController;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.json.TLJsonBuilder;
import org.telegram.tgnet.json.TLJsonParser;
import org.telegram.tgnet.tl.TL_account;
import org.telegram.tgnet.tl.TL_aicompose;
import org.telegram.tgnet.tl.TL_bots;
import org.telegram.tgnet.tl.TL_keyboard;
import org.telegram.tgnet.tl.TL_communities;
import org.telegram.tgnet.tl.TL_iv;
import org.telegram.tgnet.tl.TL_update;
import org.telegram.tgnet.tl.legacy.TL_legacy_message;
import org.telegram.tgnet.tl.TL_payments;
import org.telegram.tgnet.tl.TL_stars;
import org.telegram.tgnet.tl.TL_stats;
import org.telegram.tgnet.tl.TL_stories;
import org.telegram.ui.Components.poll.PollAttachedMediaPack;
import org.telegram.ui.Stories.MessageMediaStoryFull;
import org.telegram.ui.Stories.MessageMediaStoryFull_old;
import org.telegram.ui.community.CommunityUtils;

import java.util.ArrayList;
import java.util.HashMap;

public class TLRPC {

    //public static final int MESSAGE_FLAG_UNREAD             = 0x00000001;
    //public static final int MESSAGE_FLAG_OUT                = 0x00000002;
    public static final int MESSAGE_FLAG_FWD                = 0x00000004;
    public static final int MESSAGE_FLAG_REPLY              = 0x00000008;
    //public static final int MESSAGE_FLAG_MENTION            = 0x00000010;
    //public static final int MESSAGE_FLAG_CONTENT_UNREAD     = 0x00000020;
    public static final int MESSAGE_FLAG_HAS_MARKUP         = 0x00000040;
    public static final int MESSAGE_FLAG_HAS_ENTITIES       = 0x00000080;
    public static final int MESSAGE_FLAG_HAS_FROM_ID        = 0x00000100;
    public static final int MESSAGE_FLAG_HAS_MEDIA          = 0x00000200;
    public static final int MESSAGE_FLAG_HAS_VIEWS          = 0x00000400;
    public static final int MESSAGE_FLAG_HAS_BOT_ID         = 0x00000800;
    public static final int MESSAGE_FLAG_EDITED             = 0x00008000;

    public static final int LAYER = 229;

    public static abstract class EmailVerifyPurpose extends TLObject {

        public static EmailVerifyPurpose TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            EmailVerifyPurpose result = null;
            switch (constructor) {
                case TL_emailVerifyPurposeLoginSetup.constructor:
                    result = new TL_emailVerifyPurposeLoginSetup();
                    break;
                case TL_emailVerifyPurposeLoginChange.constructor:
                    result = new TL_emailVerifyPurposeLoginChange();
                    break;
                case TL_emailVerifyPurposePassport.constructor:
                    result = new TL_emailVerifyPurposePassport();
                    break;
            }
            return TLdeserialize(EmailVerifyPurpose.class, result, stream, constructor, exception);
        }
    }

    public static class TL_emailVerifyPurposeLoginSetup extends EmailVerifyPurpose {
        public static final int constructor = 0x4345be73;

        public String phone_number;
        public String phone_code_hash;

        public void readParams(InputSerializedData stream, boolean exception) {
            phone_number = stream.readString(exception);
            phone_code_hash = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(phone_number);
            stream.writeString(phone_code_hash);
        }
    }

    public static class TL_inputReplyToMonoForum extends InputReplyTo {
        public static final int constructor = 0x69d66c45;

        public void readParams(InputSerializedData stream, boolean exception) {
            monoforum_peer_id = InputPeer.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            monoforum_peer_id.serializeToStream(stream);
        }
    }

    public static class TL_emailVerifyPurposeLoginChange extends EmailVerifyPurpose {
        public static final int constructor = 0x527d22eb;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_emailVerifyPurposePassport extends EmailVerifyPurpose {
        public static final int constructor = 0xbbf51685;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static abstract class EmailVerification extends TLObject {

        public static EmailVerification TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            EmailVerification result = null;
            switch (constructor) {
                case TL_emailVerificationCode.constructor:
                    result = new TL_emailVerificationCode();
                    break;
                case TL_emailVerificationGoogle.constructor:
                    result = new TL_emailVerificationGoogle();
                    break;
                case TL_emailVerificationApple.constructor:
                    result = new TL_emailVerificationApple();
                    break;
            }
            return TLdeserialize(EmailVerification.class, result, stream, constructor, exception);
        }
    }

    public static class TL_emailVerificationCode extends EmailVerification {
        public static final int constructor = 0x922e55a9;

        public String code;

        public void readParams(InputSerializedData stream, boolean exception) {
            code = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(code);
        }
    }

    public static class TL_emailVerificationGoogle extends EmailVerification {
        public static final int constructor = 0xdb909ec2;

        public String token;

        public void readParams(InputSerializedData stream, boolean exception) {
            token = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(token);
        }
    }

    public static class TL_emailVerificationApple extends EmailVerification {
        public static final int constructor = 0x96d074fd;

        public String token;

        public void readParams(InputSerializedData stream, boolean exception) {
            token = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(token);
        }
    }

    public static class TL_chatBannedRights extends TLObject {
        public static final int constructor = 0x9f120418;

        public int flags;
        public boolean view_messages;
        public boolean send_messages;
        public boolean send_media;
        public boolean send_stickers;
        public boolean send_gifs;
        public boolean send_games;
        public boolean send_inline;
        public boolean embed_links;
        public boolean send_polls;
        public boolean change_info;
        public boolean invite_users;
        public boolean pin_messages;
        public boolean manage_topics;
        public boolean send_photos;
        public boolean send_videos;
        public boolean send_roundvideos;
        public boolean send_audios;
        public boolean send_voices;
        public boolean send_docs;
        public boolean send_plain;
        public boolean edit_rank;
        public boolean send_reactions;
        public boolean manage_linked_peers;
        public int until_date;

        public static TL_chatBannedRights TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_chatBannedRights result = TL_chatBannedRights.constructor != constructor ? null : new TL_chatBannedRights();
            return TLdeserialize(TL_chatBannedRights.class, result, stream, constructor, exception);
        }

        public static TL_chatBannedRights clone(TL_chatBannedRights rights) {
            if (rights == null) return null;
            SerializedData data = new SerializedData(rights.getObjectSize());
            rights.serializeToStream(data);
            data = new SerializedData(data.toByteArray());
            return TLdeserialize(data, data.readInt32(false), false);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (!(obj instanceof TL_chatBannedRights)) return false;
            return this.flags == ((TL_chatBannedRights) obj).flags;
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            view_messages = hasFlag(flags, FLAG_0);
            send_messages = hasFlag(flags, FLAG_1);
            send_media = hasFlag(flags, FLAG_2);
            send_stickers = hasFlag(flags, FLAG_3);
            send_gifs = hasFlag(flags, FLAG_4);
            send_games = hasFlag(flags, FLAG_5);
            send_inline = hasFlag(flags, FLAG_6);
            embed_links = hasFlag(flags, FLAG_7);
            send_polls = hasFlag(flags, FLAG_8);
            change_info = hasFlag(flags, FLAG_10);
            invite_users = hasFlag(flags, FLAG_15);
            pin_messages = hasFlag(flags, FLAG_17);
            manage_topics = hasFlag(flags, FLAG_18);
            send_photos = hasFlag(flags, FLAG_19);
            send_videos = hasFlag(flags, FLAG_20);
            send_roundvideos = hasFlag(flags, FLAG_21);
            send_audios = hasFlag(flags, FLAG_22);
            send_voices = hasFlag(flags, FLAG_23);
            send_docs = hasFlag(flags, FLAG_24);
            send_plain = hasFlag(flags, FLAG_25);
            edit_rank = hasFlag(flags, FLAG_26);
            send_reactions = hasFlag(flags, FLAG_27);
            manage_linked_peers = hasFlag(flags, FLAG_28);
            if (send_media) {
                send_photos = true;
                send_videos = true;
                send_roundvideos = true;
                send_audios = true;
                send_voices = true;
                send_docs = true;
            }
            until_date = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            if (ApplicationLoader.isAndroidTestEnvironment()) {
                stream.writeInt32(flags);
                stream.writeInt32(until_date);
                return;
            }

            if (send_photos && send_videos && send_roundvideos && send_audios && send_voices && send_docs) {
                send_media = true;
            } else {
                send_media = false;
            }
            if (send_plain && send_media && send_stickers) {
                send_messages = true;
            } else {
                send_messages = false;
            }
            flags = setFlag(flags, FLAG_0, view_messages);
            flags = setFlag(flags, FLAG_1, send_messages);
            flags = setFlag(flags, FLAG_2, send_media);
            flags = setFlag(flags, FLAG_3, send_stickers);
            flags = setFlag(flags, FLAG_4, send_gifs);
            flags = setFlag(flags, FLAG_5, send_games);
            flags = setFlag(flags, FLAG_6, send_inline);
            flags = setFlag(flags, FLAG_7, embed_links);
            flags = setFlag(flags, FLAG_8, send_polls);
            flags = setFlag(flags, FLAG_10, change_info);
            flags = setFlag(flags, FLAG_15, invite_users);
            flags = setFlag(flags, FLAG_17, pin_messages);
            flags = setFlag(flags, FLAG_18, manage_topics);
            flags = setFlag(flags, FLAG_19, send_photos);
            flags = setFlag(flags, FLAG_20, send_videos);
            flags = setFlag(flags, FLAG_21, send_roundvideos);
            flags = setFlag(flags, FLAG_22, send_audios);
            flags = setFlag(flags, FLAG_23, send_voices);
            flags = setFlag(flags, FLAG_24, send_docs);
            flags = setFlag(flags, FLAG_25, send_plain);
            flags = setFlag(flags, FLAG_26, edit_rank);
            flags = setFlag(flags, FLAG_27, send_reactions);
            flags = setFlag(flags, FLAG_28, manage_linked_peers);
            stream.writeInt32(flags);
            stream.writeInt32(until_date);
        }
    }

    public static class TL_stickers_suggestedShortName extends TLObject {
        public static final int constructor = 0x85fea03f;

        public String short_name;

        public static TL_stickers_suggestedShortName TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_stickers_suggestedShortName result = TL_stickers_suggestedShortName.constructor != constructor ? null : new TL_stickers_suggestedShortName();
            return TLdeserialize(TL_stickers_suggestedShortName.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            short_name = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(short_name);
        }
    }

    public static abstract class DraftMessage extends TLObject {

        public int flags;
        public boolean no_webpage;
        public boolean invert_media;
        public InputReplyTo reply_to;
        public String message;
        public ArrayList<MessageEntity> entities = new ArrayList<>();
        public InputMedia media;
        public SuggestedPost suggested_post;
        public int date;
        public long effect;
        public TL_iv.RichMessage rich_message;

        public static DraftMessage TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            DraftMessage result = null;
            switch (constructor) {
                case TL_draftMessageEmpty.constructor:
                    result = new TL_draftMessageEmpty();
                    break;
                case TL_draftMessageEmpty_layer81.constructor:
                    result = new TL_draftMessageEmpty_layer81();
                    break;
                case TL_draftMessage.constructor:
                    result = new TL_draftMessage();
                    break;
                case TL_draftMessage_layer226.constructor:
                    result = new TL_draftMessage_layer226();
                    break;
                case TL_draftMessage_layer205.constructor:
                    result = new TL_draftMessage_layer205();
                    break;
                case TL_draftMessage_layer181.constructor:
                    result = new TL_draftMessage_layer181();
                    break;
                case TL_draftMessage_layer165.constructor:
                    result = new TL_draftMessage_layer165();
                    break;
            }
            return TLdeserialize(DraftMessage.class, result, stream, constructor, exception);
        }
    }

    public static class TL_draftMessageEmpty extends DraftMessage {
        public static final int constructor = 0x1b0c841a;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_0)) {
                date = stream.readInt32(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeInt32(date);
            }
        }
    }

    public static class TL_draftMessageEmpty_layer81 extends TL_draftMessageEmpty {
        public static final int constructor = 0xba4baec5;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_draftMessage extends DraftMessage {
        public static final int constructor = 0x60fe3294;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            no_webpage = hasFlag(flags, FLAG_1);
            invert_media = hasFlag(flags, FLAG_6);
            if (hasFlag(flags, FLAG_4)) {
                reply_to = InputReplyTo.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            message = stream.readString(exception);
            if (hasFlag(flags, FLAG_3)) {
                entities = Vector.deserialize(stream, MessageEntity::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_5)) {
                media = InputMedia.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            date = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_7)) {
                effect = stream.readInt64(exception);
            }
            if (hasFlag(flags, FLAG_8)) {
                suggested_post = SuggestedPost.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_9)) {
                rich_message = TL_iv.RichMessage.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_1, no_webpage);
            flags = setFlag(flags, FLAG_6, invert_media);
            flags = setFlag(flags, FLAG_8, suggested_post != null);
            flags = setFlag(flags, FLAG_9, rich_message != null);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_4)) {
                reply_to.serializeToStream(stream);
            }
            stream.writeString(message);
            if (hasFlag(flags, FLAG_3)) {
                Vector.serialize(stream, entities);
            }
            if (hasFlag(flags, FLAG_5)) {
                media.serializeToStream(stream);
            }
            stream.writeInt32(date);
            if (hasFlag(flags, FLAG_7)) {
                stream.writeInt64(effect);
            }
            if (hasFlag(flags, FLAG_8)) {
                suggested_post.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_9)) {
                rich_message.serializeToStream(stream);
            }
        }
    }

    public static class TL_draftMessage_layer226 extends TL_draftMessage {
        public static final int constructor = 0x96eaa5eb;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            no_webpage = hasFlag(flags, FLAG_1);
            invert_media = hasFlag(flags, FLAG_6);
            if (hasFlag(flags, FLAG_4)) {
                reply_to = InputReplyTo.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            message = stream.readString(exception);
            if (hasFlag(flags, FLAG_3)) {
                entities = Vector.deserialize(stream, MessageEntity::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_5)) {
                media = InputMedia.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            date = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_7)) {
                effect = stream.readInt64(exception);
            }
            if (hasFlag(flags, FLAG_8)) {
                suggested_post = SuggestedPost.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_1, no_webpage);
            flags = setFlag(flags, FLAG_6, invert_media);
            flags = setFlag(flags, FLAG_8, suggested_post != null);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_4)) {
                reply_to.serializeToStream(stream);
            }
            stream.writeString(message);
            if (hasFlag(flags, FLAG_3)) {
                Vector.serialize(stream, entities);
            }
            if (hasFlag(flags, FLAG_5)) {
                media.serializeToStream(stream);
            }
            stream.writeInt32(date);
            if (hasFlag(flags, FLAG_7)) {
                stream.writeInt64(effect);
            }
            if (hasFlag(flags, FLAG_8)) {
                suggested_post.serializeToStream(stream);
            }
        }
    }

    public static class TL_draftMessage_layer205 extends DraftMessage {
        public static final int constructor = 0x2d65321f;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            no_webpage = hasFlag(flags, FLAG_1);
            invert_media = hasFlag(flags, FLAG_6);
            if (hasFlag(flags, FLAG_4)) {
                reply_to = InputReplyTo.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            message = stream.readString(exception);
            if (hasFlag(flags, FLAG_3)) {
                entities = Vector.deserialize(stream, MessageEntity::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_5)) {
                media = InputMedia.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            date = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_7)) {
                effect = stream.readInt64(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_1, no_webpage);
            flags = setFlag(flags, FLAG_6, invert_media);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_4)) {
                reply_to.serializeToStream(stream);
            }
            stream.writeString(message);
            if (hasFlag(flags, FLAG_3)) {
                Vector.serialize(stream, entities);
            }
            if (hasFlag(flags, FLAG_5)) {
                media.serializeToStream(stream);
            }
            stream.writeInt32(date);
            if (hasFlag(flags, FLAG_7)) {
                stream.writeInt64(effect);
            }
        }
    }

    public static class TL_draftMessage_layer181 extends TL_draftMessage {
        public static final int constructor = 0x3fccf7ef;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            no_webpage = hasFlag(flags, FLAG_1);
            invert_media = hasFlag(flags, FLAG_6);
            if (hasFlag(flags, FLAG_4)) {
                reply_to = InputReplyTo.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            message = stream.readString(exception);
            if (hasFlag(flags, FLAG_3)) {
                entities = Vector.deserialize(stream, MessageEntity::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_5)) {
                media = InputMedia.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            date = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_1, no_webpage);
            flags = setFlag(flags, FLAG_6, invert_media);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_4)) {
                reply_to.serializeToStream(stream);
            }
            stream.writeString(message);
            if (hasFlag(flags, FLAG_3)) {
                Vector.serialize(stream, entities);
            }
            if (hasFlag(flags, FLAG_5)) {
                media.serializeToStream(stream);
            }
            stream.writeInt32(date);
        }
    }

    public static class TL_draftMessage_layer165 extends TL_draftMessage {
        public static final int constructor = 0xfd8e711f;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            no_webpage = hasFlag(flags, FLAG_1);
            if (hasFlag(flags, FLAG_0)) {
                TL_inputReplyToMessage reply_to = new TL_inputReplyToMessage();
                reply_to.flags |= 16;
                reply_to.reply_to_msg_id = stream.readInt32(exception);
                this.reply_to = reply_to;
            }
            message = stream.readString(exception);
            if (hasFlag(flags, FLAG_3)) {
                entities = Vector.deserialize(stream, MessageEntity::TLdeserialize, exception);
            }
            date = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_1, no_webpage);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeInt32(reply_to instanceof TLRPC.TL_inputReplyToMessage ? ((TL_inputReplyToMessage) reply_to).reply_to_msg_id : 0);
            }
            stream.writeString(message);
            if (hasFlag(flags, FLAG_3)) {
                Vector.serialize(stream, entities);
            }
            stream.writeInt32(date);
        }
    }

    public static abstract class ChatPhoto extends TLObject {

        public int flags;
        public boolean has_video;
        public FileLocation photo_small;
        public FileLocation photo_big;
        public byte[] stripped_thumb;
        public int dc_id;
        public long photo_id;
        public BitmapDrawable strippedBitmap;

        public static ChatPhoto TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            return TLdeserialize(stream, constructor, exception, true);
        }

        public static ChatPhoto TLdeserialize(InputSerializedData stream, int constructor, boolean exception,  boolean allowStripedThumb) {
            ChatPhoto result = null;
            switch (constructor) {
                case 0x1c6e1c11:
                    result = new TL_chatPhoto();
                    break;
                case 0x475cdbd5:
                    result = new TL_chatPhoto_layer115();
                    break;
                case 0x37c1011c:
                    result = new TL_chatPhotoEmpty();
                    break;
                case 0x6153276a:
                    result = new TL_chatPhoto_layer97();
                    break;
                case 0xd20b9f3c:
                    result = new TL_chatPhoto_layer126();
                    break;
                case 0x4790ee05:
                    result = new TL_chatPhoto_layer127();
                    break;
            }
            return TLdeserialize(ChatPhoto.class, result, stream, constructor, exception);
        }
    }

    public static class TL_chatPhoto extends ChatPhoto {
        public static final int constructor = 0x1c6e1c11;

        public void readParams(InputSerializedData stream, boolean exception) {
            readParams(stream, exception, true);
        }

        public void readParams(InputSerializedData stream, boolean exception, boolean allowStripedThumbs) {
            flags = stream.readInt32(exception);
            has_video = hasFlag(flags, FLAG_0);
            photo_id = stream.readInt64(exception);
            if (hasFlag(flags, FLAG_1)) {
                stripped_thumb = stream.readByteArray(exception);
            }
            dc_id = stream.readInt32(exception);
            photo_small = new TL_fileLocationToBeDeprecated();
            photo_small.volume_id = -photo_id;
            photo_small.local_id = 'a';
            photo_big = new TL_fileLocationToBeDeprecated();
            photo_big.volume_id = -photo_id;
            photo_big.local_id = 'c';

            if (allowStripedThumbs && stripped_thumb != null) {
                try {
                    strippedBitmap = new BitmapDrawable(ImageLoader.getStrippedPhotoBitmap(stripped_thumb, "b"));
                } catch (Throwable e) {
                    FileLog.e(e);
                }
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, has_video);
            stream.writeInt32(flags);
            stream.writeInt64(photo_id);
            if (hasFlag(flags, FLAG_1)) {
                stream.writeByteArray(stripped_thumb);
            }
            stream.writeInt32(dc_id);
        }
    }

    public static class TL_chatPhoto_layer115 extends TL_chatPhoto {
        public static final int constructor = 0x475cdbd5;

        public void readParams(InputSerializedData stream, boolean exception) {
            photo_small = FileLocation.TLdeserialize(stream, stream.readInt32(exception), exception);
            photo_big = FileLocation.TLdeserialize(stream, stream.readInt32(exception), exception);
            dc_id = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            photo_small.serializeToStream(stream);
            photo_big.serializeToStream(stream);
            stream.writeInt32(dc_id);
        }
    }

    public static class TL_chatPhotoEmpty extends ChatPhoto {
        public static final int constructor = 0x37c1011c;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_chatPhoto_layer97 extends TL_chatPhoto {
        public static final int constructor = 0x6153276a;

        public void readParams(InputSerializedData stream, boolean exception) {
            photo_small = FileLocation.TLdeserialize(stream, stream.readInt32(exception), exception);
            photo_big = FileLocation.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            photo_small.serializeToStream(stream);
            photo_big.serializeToStream(stream);
        }
    }

    public static class TL_chatPhoto_layer126 extends TL_chatPhoto {
        public static final int constructor = 0xd20b9f3c;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            has_video = hasFlag(flags, FLAG_0);
            photo_small = FileLocation.TLdeserialize(stream, stream.readInt32(exception), exception);
            photo_big = FileLocation.TLdeserialize(stream, stream.readInt32(exception), exception);
            dc_id = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, has_video);
            stream.writeInt32(flags);
            photo_small.serializeToStream(stream);
            photo_big.serializeToStream(stream);
            stream.writeInt32(dc_id);
        }
    }

    public static class TL_chatPhoto_layer127 extends TL_chatPhoto {
        public static final int constructor = 0x4790ee05;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            has_video = hasFlag(flags, FLAG_0);
            photo_small = FileLocation.TLdeserialize(stream, stream.readInt32(exception), exception);
            photo_big = FileLocation.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_1)) {
                stripped_thumb = stream.readByteArray(exception);
                try {
                    strippedBitmap = new BitmapDrawable(ImageLoader.getStrippedPhotoBitmap(stripped_thumb, "b"));
                } catch (Throwable e) {
                    FileLog.e(e);
                }
            }
            dc_id = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, has_video);
            stream.writeInt32(flags);
            photo_small.serializeToStream(stream);
            photo_big.serializeToStream(stream);
            if (hasFlag(flags, FLAG_1)) {
                stream.writeByteArray(stripped_thumb);
            }
            stream.writeInt32(dc_id);
        }
    }

    public static class TL_help_termsOfService extends TLObject {
        public static final int constructor = 0x780a0310;

        public int flags;
        public boolean popup;
        public TL_dataJSON id;
        public String text;
        public ArrayList<MessageEntity> entities = new ArrayList<>();
        public int min_age_confirm;

        public static TL_help_termsOfService TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_help_termsOfService result = TL_help_termsOfService.constructor != constructor ? null : new TL_help_termsOfService();
            return TLdeserialize(TL_help_termsOfService.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            popup = hasFlag(flags, FLAG_0);
            id = TL_dataJSON.TLdeserialize(stream, stream.readInt32(exception), exception);
            text = stream.readString(exception);
            entities = Vector.deserialize(stream, MessageEntity::TLdeserialize, exception);
            if (hasFlag(flags, FLAG_1)) {
                min_age_confirm = stream.readInt32(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, popup);
            stream.writeInt32(flags);
            id.serializeToStream(stream);
            stream.writeString(text);
            Vector.serialize(stream, entities);
            if (hasFlag(flags, FLAG_1)) {
                stream.writeInt32(min_age_confirm);
            }
        }
    }

    public static class PaymentReceipt extends TLObject {
        public int flags;
        public int date;
        public long bot_id;
        public long provider_id;
        public String title;
        public String description;
        public WebDocument photo;
        public TL_invoice invoice;
        public TL_paymentRequestedInfo info;
        public TL_shippingOption shipping;
        public long tip_amount;
        public String currency;
        public long total_amount;
        public String credentials_title;
        public ArrayList<User> users = new ArrayList<>();
        public String transaction_id;

        public static PaymentReceipt TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            PaymentReceipt result = null;
            switch (constructor) {
                case TL_payments_paymentReceipt.constructor:
                    result = new TL_payments_paymentReceipt();
                    break;
                case TL_payments_paymentReceiptStars.constructor:
                    result = new TL_payments_paymentReceiptStars();
                    break;
            }
            return TLdeserialize(PaymentReceipt.class, result, stream, constructor, exception);
        }
    }

    public static class TL_payments_paymentReceipt extends PaymentReceipt {
        public static final int constructor = 0x70c4fe03;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            date = stream.readInt32(exception);
            bot_id = stream.readInt64(exception);
            provider_id = stream.readInt64(exception);
            title = stream.readString(exception);
            description = stream.readString(exception);
            if (hasFlag(flags, FLAG_2)) {
                photo = WebDocument.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            invoice = TL_invoice.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_0)) {
                info = TL_paymentRequestedInfo.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_1)) {
                shipping = TL_shippingOption.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_3)) {
                tip_amount = stream.readInt64(exception);
            }
            currency = stream.readString(exception);
            total_amount = stream.readInt64(exception);
            credentials_title = stream.readString(exception);
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            stream.writeInt32(date);
            stream.writeInt64(bot_id);
            stream.writeInt64(provider_id);
            stream.writeString(title);
            stream.writeString(description);
            if (hasFlag(flags, FLAG_2)) {
                photo.serializeToStream(stream);
            }
            invoice.serializeToStream(stream);
            if (hasFlag(flags, FLAG_0)) {
                info.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_1)) {
                shipping.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_3)) {
                stream.writeInt64(tip_amount);
            }
            stream.writeString(currency);
            stream.writeInt64(total_amount);
            stream.writeString(credentials_title);
            Vector.serialize(stream, users);
        }
    }

    public static class TL_payments_paymentReceiptStars extends PaymentReceipt {
        public static final int constructor = 0xdabbf83a;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            date = stream.readInt32(exception);
            bot_id = stream.readInt64(exception);
            title = stream.readString(exception);
            description = stream.readString(exception);
            if (hasFlag(flags, FLAG_2)) {
                photo = WebDocument.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            invoice = TL_invoice.TLdeserialize(stream, stream.readInt32(exception), exception);
            currency = stream.readString(exception);
            total_amount = stream.readInt64(exception);
            transaction_id = stream.readString(exception);
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            stream.writeInt32(date);
            stream.writeInt64(bot_id);
            stream.writeString(title);
            stream.writeString(description);
            if (hasFlag(flags, FLAG_2)) {
                photo.serializeToStream(stream);
            }
            invoice.serializeToStream(stream);
            stream.writeString(currency);
            stream.writeInt64(total_amount);
            stream.writeString(transaction_id);
            Vector.serialize(stream, users);
        }
    }

    public static abstract class NotifyPeer extends TLObject {

        public static NotifyPeer TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            return TLdeserialize(NotifyPeer.class, fromConstructor(constructor), stream, constructor, exception);
        }

        private static NotifyPeer fromConstructor(int constructor) {
            switch (constructor) {
                case TL_notifyForumTopic.constructor:
                    return new TL_notifyForumTopic();
                case TL_notifyBroadcasts.constructor:
                    return new TL_notifyBroadcasts();
                case TL_notifyChats.constructor:
                    return new TL_notifyChats();
                case TL_notifyUsers.constructor:
                    return new TL_notifyUsers();
                case TL_notifyPeer.constructor:
                    return new TL_notifyPeer();
                case TL_notifyCommunity.constructor:
                    return new TL_notifyCommunity();
                default:
                    return null;
            }
        }
    }

    public static class TL_notifyForumTopic extends NotifyPeer {
        public static final int constructor = 0x226e6308;

        public Peer peer;
        public int top_msg_id;

        public void readParams(InputSerializedData stream, boolean exception) {
            peer = Peer.TLdeserialize(stream, stream.readInt32(exception), exception);
            top_msg_id = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            peer.serializeToStream(stream);
            stream.writeInt32(top_msg_id);
        }
    }

    public static class TL_notifyCommunity extends NotifyPeer {
        public static final int constructor = 0xBE376999;

        public long community_id;

        public void readParams(InputSerializedData stream, boolean exception) {
            community_id = stream.readInt64(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(community_id);
        }
    }

    public static class TL_notifyBroadcasts extends NotifyPeer {
        public static final int constructor = 0xd612e8ef;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_notifyChats extends NotifyPeer {
        public static final int constructor = 0xc007cec3;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_notifyUsers extends NotifyPeer {
        public static final int constructor = 0xb4c83b4c;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_notifyPeer extends NotifyPeer {
        public static final int constructor = 0x9fd40bd8;

        public Peer peer;

        public void readParams(InputSerializedData stream, boolean exception) {
            peer = Peer.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            peer.serializeToStream(stream);
        }
    }

    public static class TL_emojiKeywordsDifference extends TLObject {
        public static final int constructor = 0x5cc761bd;

        public String lang_code;
        public int from_version;
        public int version;
        public ArrayList<EmojiKeyword> keywords = new ArrayList<>();

        public static TL_emojiKeywordsDifference TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_emojiKeywordsDifference result = TL_emojiKeywordsDifference.constructor != constructor ? null : new TL_emojiKeywordsDifference();
            return TLdeserialize(TL_emojiKeywordsDifference.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            lang_code = stream.readString(exception);
            from_version = stream.readInt32(exception);
            version = stream.readInt32(exception);
            keywords = Vector.deserialize(stream, EmojiKeyword::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(lang_code);
            stream.writeInt32(from_version);
            stream.writeInt32(version);
            Vector.serialize(stream, keywords);
        }
    }

    public static abstract class messages_SentEncryptedMessage extends TLObject {
        public int date;
        public EncryptedFile file;

        public static messages_SentEncryptedMessage TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            messages_SentEncryptedMessage result = null;
            switch (constructor) {
                case 0x560f8935:
                    result = new TL_messages_sentEncryptedMessage();
                    break;
                case 0x9493ff32:
                    result = new TL_messages_sentEncryptedFile();
                    break;
            }
            return TLdeserialize(messages_SentEncryptedMessage.class, result, stream, constructor, exception);
        }
    }

    public static class TL_messages_sentEncryptedMessage extends messages_SentEncryptedMessage {
        public static final int constructor = 0x560f8935;

        public void readParams(InputSerializedData stream, boolean exception) {
            date = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(date);
        }
    }

    public static class TL_messages_sentEncryptedFile extends messages_SentEncryptedMessage {
        public static final int constructor = 0x9493ff32;

        public void readParams(InputSerializedData stream, boolean exception) {
            date = stream.readInt32(exception);
            file = EncryptedFile.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(date);
            file.serializeToStream(stream);
        }
    }

    public static class TL_premiumSubscriptionOption extends TLObject {
        public static final int constructor = 0x5f2d1df2;

        public int flags;
        public boolean current;
        public String transaction;
        public boolean can_purchase_upgrade;
        public int months;
        public String currency;
        public long amount;
        public String bot_url;
        public String store_product;

        public static TL_premiumSubscriptionOption TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            TL_premiumSubscriptionOption result = null;
            switch (constructor) {
                case 0x5f2d1df2:
                    result = new TL_premiumSubscriptionOption();
                    break;
                case 0xb6f11ebe:
                    result = new TL_premiumSubscriptionOption_layer151();
                    break;
            }
            return TLdeserialize(TL_premiumSubscriptionOption.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            current = hasFlag(flags, FLAG_1);
            if (hasFlag(flags, FLAG_3)) {
                transaction = stream.readString(exception);
            }
            can_purchase_upgrade = hasFlag(flags, FLAG_2);
            months = stream.readInt32(exception);
            currency = stream.readString(exception);
            amount = stream.readInt64(exception);
            bot_url = stream.readString(exception);
            if (hasFlag(flags, FLAG_0)) {
                store_product = stream.readString(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_1, current);
            flags = setFlag(flags, FLAG_2, can_purchase_upgrade);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_3)) {
                stream.writeString(transaction);
            }
            stream.writeInt32(months);
            stream.writeString(currency);
            stream.writeInt64(amount);
            stream.writeString(bot_url);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeString(store_product);
            }
        }
    }

    public static class TL_premiumSubscriptionOption_layer151 extends TL_premiumSubscriptionOption {
        public static final int constructor = 0xb6f11ebe;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            months = stream.readInt32(exception);
            currency = stream.readString(exception);
            amount = stream.readInt64(exception);
            bot_url = stream.readString(exception);
            if (hasFlag(flags, FLAG_0)) {
                store_product = stream.readString(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            stream.writeInt32(months);
            stream.writeString(currency);
            stream.writeInt64(amount);
            stream.writeString(bot_url);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeString(store_product);
            }
        }
    }

    public static class TL_premiumGiftOption extends TLObject {
        public static final int constructor = 0x79c059f7;

        public int flags;
        public int months;
        public String currency;
        public long amount;
        public String bot_url;
        public String store_product;

        public static TL_premiumGiftOption TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            TL_premiumGiftOption result = null;
            switch (constructor) {
                case TL_premiumGiftOption.constructor:
                    result = new TL_premiumGiftOption();
                    break;
                case TL_premiumGiftOption_layer199.constructor:
                    result = new TL_premiumGiftOption_layer199();
                    break;
            }
            return TLdeserialize(TL_premiumGiftOption.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            months = stream.readInt32(exception);
            currency = stream.readString(exception);
            amount = stream.readInt64(exception);
            if (hasFlag(flags, FLAG_1)) {
                bot_url = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_0)) {
                store_product = stream.readString(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            stream.writeInt32(months);
            stream.writeString(currency);
            stream.writeInt64(amount);
            if (hasFlag(flags, FLAG_1)) {
                stream.writeString(bot_url);
            }
            if (hasFlag(flags, FLAG_0)) {
                stream.writeString(store_product);
            }
        }
    }

    public static class TL_premiumGiftOption_layer199 extends TL_premiumGiftOption {
        public static final int constructor = 0x74c34319;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            months = stream.readInt32(exception);
            currency = stream.readString(exception);
            amount = stream.readInt64(exception);
            bot_url = stream.readString(exception);
            if (hasFlag(flags, FLAG_0)) {
                store_product = stream.readString(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            stream.writeInt32(months);
            stream.writeString(currency);
            stream.writeInt64(amount);
            stream.writeString(bot_url);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeString(store_product);
            }
        }
    }

    public static class TL_error extends TLObject {
        public static final int constructor = 0xc4b9f9bb;

        public int code;
        public String text;

        public static TL_error TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_error result = TL_error.constructor != constructor ? null : new TL_error();
            return TLdeserialize(TL_error.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            code = stream.readInt32(exception);
            text = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(code);
            stream.writeString(text);
        }
    }

    public static abstract class UrlAuthResult extends TLObject {

        public static UrlAuthResult TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            UrlAuthResult result = null;
            switch (constructor) {
                case TL_urlAuthResultDefault.constructor:
                    result = new TL_urlAuthResultDefault();
                    break;
                case TL_urlAuthResultRequest.constructor:
                    result = new TL_urlAuthResultRequest();
                    break;
                case TL_urlAuthResultAccepted.constructor:
                    result = new TL_urlAuthResultAccepted();
                    break;
            }
            return TLdeserialize(UrlAuthResult.class, result, stream, constructor, exception);
        }
    }

    public static class TL_urlAuthResultDefault extends UrlAuthResult {
        public static final int constructor = 0xa9d6db1f;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_messages_checkUrlAuthMatchCode extends TLMethod<Bool> {
        public static final int constructor = 0xC9A47B0B;

        public String url;
        public String match_code;

        @Override
        public Bool deserializeResponseT(InputSerializedData stream, int constructor, boolean exception) {
            return Bool.TLdeserialize(stream, constructor, exception);
        }

        @Override
        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(url);
            stream.writeString(match_code);
        }
    }

    public static class TL_urlAuthResultRequest extends UrlAuthResult {
        public static final int constructor = 0x3cd623ec;

        public int flags;
        public boolean request_write_access;
        public boolean request_phone_number;
        public boolean match_codes_first;
        public boolean is_app;
        public User bot;
        public String domain;
        public String browser;
        public String platform;
        public String ip;
        public String region;
        public ArrayList<String> match_codes = new ArrayList<>();
        public long user_id_hint;
        public String verified_app_name;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            request_write_access = hasFlag(flags, FLAG_0);
            request_phone_number = hasFlag(flags, FLAG_1);
            match_codes_first = hasFlag(flags, FLAG_5);
            is_app = hasFlag(flags, FLAG_6);
            bot = User.TLdeserialize(stream, stream.readInt32(exception), exception);
            domain = stream.readString(exception);
            if (hasFlag(flags, FLAG_2)) {
                browser = stream.readString(exception);
                platform = stream.readString(exception);
                ip = stream.readString(exception);
                region = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_3)) {
                match_codes = Vector.deserializeString(stream, exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                user_id_hint = stream.readInt64(exception);
            }
            if (hasFlag(flags, FLAG_7)) {
                verified_app_name = stream.readString(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, request_write_access);
            flags = setFlag(flags, FLAG_1, request_phone_number);
            flags = setFlag(flags, FLAG_5, match_codes_first);
            flags = setFlag(flags, FLAG_6, is_app);
            stream.writeInt32(flags);
            bot.serializeToStream(stream);
            stream.writeString(domain);
            if (hasFlag(flags, FLAG_2)) {
                stream.writeString(browser);
                stream.writeString(platform);
                stream.writeString(ip);
                stream.writeString(region);
            }
            if (hasFlag(flags, FLAG_3)) {
                Vector.serializeString(stream, match_codes);
            }
            if (hasFlag(flags, FLAG_4)) {
                stream.writeInt64(user_id_hint);
            }
            if (hasFlag(flags, FLAG_7)) {
                stream.writeString(verified_app_name);
            }
        }
    }

    public static class TL_urlAuthResultAccepted extends UrlAuthResult {
        public static final int constructor = 0x623a8fa0;

        public int flags;
        public String url;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_0)) {
                url = stream.readString(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeString(url);
            }
        }
    }

    public static class TL_messages_chatFull extends TLObject {
        public static final int constructor = 0xe5d7d19c;

        public ChatFull full_chat;
        public ArrayList<Chat> chats = new ArrayList<>();
        public ArrayList<User> users = new ArrayList<>();

        public static TL_messages_chatFull TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_messages_chatFull result = TL_messages_chatFull.constructor != constructor ? null : new TL_messages_chatFull();
            return TLdeserialize(TL_messages_chatFull.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            full_chat = ChatFull.TLdeserialize(stream, stream.readInt32(exception), exception);
            chats = Vector.deserialize(stream, Chat::TLdeserialize, exception);
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            full_chat.serializeToStream(stream);
            Vector.serialize(stream, chats);
            Vector.serialize(stream, users);
        }
    }

    public static abstract class DocumentAttribute extends TLObject {
        public String alt;
        public InputStickerSet stickerset;
        public double duration;
        public int flags;
        public TL_maskCoords mask_coords;
        public boolean round_message;
        public boolean supports_streaming;
        public String file_name;
        public int w;
        public int h;
        public boolean mask;
        public String title;
        public String performer;
        public boolean voice;
        public byte[] waveform;
        public int preload_prefix_size;
        public double video_start_ts;
        public boolean nosound;
        public String video_codec;

        public static DocumentAttribute TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            DocumentAttribute result = null;
            switch (constructor) {
                case 0x3a556302:
                    result = new TL_documentAttributeSticker_layer55();
                    break;
                case 0xef02ce6:
                    result = new TL_documentAttributeVideo_layer159();
                    break;
                case 0x51448e5:
                    result = new TL_documentAttributeAudio_old();
                    break;
                case 0x6319d612:
                    result = new TL_documentAttributeSticker();
                    break;
                case 0x11b58939:
                    result = new TL_documentAttributeAnimated();
                    break;
                case 0x15590068:
                    result = new TL_documentAttributeFilename();
                    break;
                case TL_documentAttributeVideo.constructor:
                    result = new TL_documentAttributeVideo();
                    break;
                case TL_documentAttributeVideo_layer187.constructor:
                    result = new TL_documentAttributeVideo_layer187();
                    break;
                case TL_documentAttributeVideo_layer184.constructor:
                    result = new TL_documentAttributeVideo_layer184();
                    break;
                case 0x5910cccb:
                    result = new TL_documentAttributeVideo_layer65();
                    break;
                case 0xded218e0:
                    result = new TL_documentAttributeAudio_layer45();
                    break;
                case 0xfb0a5727:
                    result = new TL_documentAttributeSticker_old();
                    break;
                case 0x9801d2f7:
                    result = new TL_documentAttributeHasStickers();
                    break;
                case 0x994c9882:
                    result = new TL_documentAttributeSticker_old2();
                    break;
                case 0x6c37c15c:
                    result = new TL_documentAttributeImageSize();
                    break;
                case 0x9852f9c6:
                    result = new TL_documentAttributeAudio();
                    break;
                case 0xfd149899:
                    result = new TL_documentAttributeCustomEmoji();
                    break;
            }
            return TLdeserialize(DocumentAttribute.class, result, stream, constructor, exception);
        }
    }

    public static class TL_documentAttributeSticker_layer55 extends TL_documentAttributeSticker {
        public static final int constructor = 0x3a556302;

        public void readParams(InputSerializedData stream, boolean exception) {
            alt = stream.readString(exception);
            stickerset = InputStickerSet.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(alt);
            stickerset.serializeToStream(stream);
        }
    }

    public static class TL_documentAttributeAudio_old extends TL_documentAttributeAudio {
        public static final int constructor = 0x51448e5;

        public void readParams(InputSerializedData stream, boolean exception) {
            duration = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32((int) duration);
        }
    }

    public static class TL_documentAttributeSticker extends DocumentAttribute {
        public static final int constructor = 0x6319d612;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            mask = hasFlag(flags, FLAG_1);
            alt = stream.readString(exception);
            stickerset = InputStickerSet.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_0)) {
                mask_coords = TL_maskCoords.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_1, mask);
            stream.writeInt32(flags);
            stream.writeString(alt);
            stickerset.serializeToStream(stream);
            if (hasFlag(flags, FLAG_0)) {
                mask_coords.serializeToStream(stream);
            }
        }
    }

    public static class TL_documentAttributeAnimated extends DocumentAttribute {
        public static final int constructor = 0x11b58939;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_documentAttributeFilename extends DocumentAttribute {
        public static final int constructor = 0x15590068;

        public void readParams(InputSerializedData stream, boolean exception) {
            file_name = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(file_name);
        }
    }

    public static class TL_documentAttributeVideo extends DocumentAttribute {
        public static final int constructor = 0x43c57c48;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            round_message = hasFlag(flags, FLAG_0);
            supports_streaming = hasFlag(flags, FLAG_1);
            nosound = hasFlag(flags, FLAG_3);
            duration = stream.readDouble(exception);
            w = stream.readInt32(exception);
            h = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_2)) {
                preload_prefix_size = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                video_start_ts = stream.readDouble(exception);
            }
            if (hasFlag(flags, FLAG_5)) {
                video_codec = stream.readString(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, round_message);
            flags = setFlag(flags, FLAG_1, supports_streaming);
            flags = setFlag(flags, FLAG_3, nosound);
            stream.writeInt32(flags);
            stream.writeDouble(duration);
            stream.writeInt32(w);
            stream.writeInt32(h);
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(preload_prefix_size);
            }
            if (hasFlag(flags, FLAG_4)) {
                stream.writeDouble(video_start_ts);
            }
            if (hasFlag(flags, FLAG_5)) {
                stream.writeString(video_codec);
            }
        }
    }

    public static class TL_documentAttributeVideo_layer187 extends TL_documentAttributeVideo {
        public static final int constructor = 0x17399fad;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            round_message = hasFlag(flags, FLAG_0);
            supports_streaming = hasFlag(flags, FLAG_1);
            nosound = hasFlag(flags, FLAG_3);
            duration = stream.readDouble(exception);
            w = stream.readInt32(exception);
            h = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_2)) {
                preload_prefix_size = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                video_start_ts = stream.readDouble(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, round_message);
            flags = setFlag(flags, FLAG_1, supports_streaming);
            flags = setFlag(flags, FLAG_3, nosound);
            stream.writeInt32(flags);
            stream.writeDouble(duration);
            stream.writeInt32(w);
            stream.writeInt32(h);
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(preload_prefix_size);
            }
            if (hasFlag(flags, FLAG_4)) {
                stream.writeDouble(video_start_ts);
            }
        }
    }

    public static class TL_documentAttributeVideo_layer184 extends TL_documentAttributeVideo {
        public static final int constructor = 0xd38ff1c2;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            round_message = hasFlag(flags, FLAG_0);
            supports_streaming = hasFlag(flags, FLAG_1);
            nosound = hasFlag(flags, FLAG_3);
            duration = stream.readDouble(exception);
            w = stream.readInt32(exception);
            h = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_2)) {
                preload_prefix_size = stream.readInt32(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, round_message);
            flags = setFlag(flags, FLAG_1, supports_streaming);
            flags = setFlag(flags, FLAG_3, nosound);
            stream.writeInt32(flags);
            stream.writeDouble(duration);
            stream.writeInt32(w);
            stream.writeInt32(h);
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(preload_prefix_size);
            }
        }
    }

    public static class TL_documentAttributeVideo_layer159 extends TL_documentAttributeVideo {
        public static final int constructor = 0xef02ce6;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            round_message = hasFlag(flags, FLAG_0);
            supports_streaming = hasFlag(flags, FLAG_1);
            duration = stream.readInt32(exception);
            w = stream.readInt32(exception);
            h = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, round_message);
            flags = setFlag(flags, FLAG_1, supports_streaming);
            stream.writeInt32(flags);
            stream.writeInt32((int) duration);
            stream.writeInt32(w);
            stream.writeInt32(h);
        }
    }

    public static class TL_documentAttributeVideo_layer65 extends TL_documentAttributeVideo {
        public static final int constructor = 0x5910cccb;

        public void readParams(InputSerializedData stream, boolean exception) {
            duration = stream.readInt32(exception);
            w = stream.readInt32(exception);
            h = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32((int) duration);
            stream.writeInt32(w);
            stream.writeInt32(h);
        }
    }

    public static class TL_documentAttributeAudio_layer45 extends TL_documentAttributeAudio {
        public static final int constructor = 0xded218e0;

        public void readParams(InputSerializedData stream, boolean exception) {
            duration = stream.readInt32(exception);
            title = stream.readString(exception);
            performer = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32((int) duration);
            stream.writeString(title);
            stream.writeString(performer);
        }
    }

    public static class TL_documentAttributeSticker_old extends TL_documentAttributeSticker {
        public static final int constructor = 0xfb0a5727;

        public void readParams(InputSerializedData stream, boolean exception) {
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_documentAttributeHasStickers extends DocumentAttribute {
        public static final int constructor = 0x9801d2f7;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_documentAttributeSticker_old2 extends TL_documentAttributeSticker {
        public static final int constructor = 0x994c9882;

        public void readParams(InputSerializedData stream, boolean exception) {
            alt = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(alt);
        }
    }

    public static class TL_documentAttributeImageSize extends DocumentAttribute {
        public static final int constructor = 0x6c37c15c;

        public void readParams(InputSerializedData stream, boolean exception) {
            w = stream.readInt32(exception);
            h = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(w);
            stream.writeInt32(h);
        }
    }

    public static class TL_documentAttributeAudio extends DocumentAttribute {
        public static final int constructor = 0x9852f9c6;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            voice = hasFlag(flags, FLAG_10);
            duration = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_0)) {
                title = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_1)) {
                performer = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                waveform = stream.readByteArray(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_10, voice);
            stream.writeInt32(flags);
            stream.writeInt32((int) duration);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeString(title);
            }
            if (hasFlag(flags, FLAG_1)) {
                stream.writeString(performer);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeByteArray(waveform);
            }
        }
    }

    public static class TL_documentAttributeCustomEmoji extends DocumentAttribute {
        public static final int constructor = 0xfd149899;

        public boolean free;
        public boolean text_color;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            free = hasFlag(flags, FLAG_0);
            text_color = hasFlag(flags, FLAG_1);
            alt = stream.readString(exception);
            stickerset = InputStickerSet.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, free);
            flags = setFlag(flags, FLAG_1, text_color);
            stream.writeInt32(flags);
            stream.writeString(alt);
            stickerset.serializeToStream(stream);
        }
    }

    public static class TL_statsURL extends TLObject {
        public static final int constructor = 0x47a971e0;

        public String url;

        public static TL_statsURL TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_statsURL result = TL_statsURL.constructor != constructor ? null : new TL_statsURL();
            return TLdeserialize(TL_statsURL.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            url = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(url);
        }
    }

    public static class TL_popularContact extends TLObject {
        public static final int constructor = 0x5ce14175;

        public long client_id;
        public int importers;

        public static TL_popularContact TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_popularContact result = TL_popularContact.constructor != constructor ? null : new TL_popularContact();
            return TLdeserialize(TL_popularContact.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            client_id = stream.readInt64(exception);
            importers = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(client_id);
            stream.writeInt32(importers);
        }
    }

    public static class TL_messages_botCallbackAnswer extends TLObject {
        public static final int constructor = 0x36585ea4;

        public int flags;
        public boolean alert;
        public boolean has_url;
        public boolean native_ui;
        public String message;
        public String url;
        public int cache_time;

        public static TL_messages_botCallbackAnswer TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_messages_botCallbackAnswer result = TL_messages_botCallbackAnswer.constructor != constructor ? null : new TL_messages_botCallbackAnswer();
            return TLdeserialize(TL_messages_botCallbackAnswer.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            alert = hasFlag(flags, FLAG_1);
            has_url = hasFlag(flags, FLAG_3);
            native_ui = hasFlag(flags, FLAG_4);
            if (hasFlag(flags, FLAG_0)) {
                message = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                url = stream.readString(exception);
            }
            cache_time = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_1, alert);
            flags = setFlag(flags, FLAG_3, has_url);
            flags = setFlag(flags, FLAG_4, native_ui);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeString(message);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeString(url);
            }
            stream.writeInt32(cache_time);
        }
    }

    public static class TL_contactStatus extends TLObject {
        public static final int constructor = 0x16d9703b;

        public long user_id;
        public UserStatus status;

        public static TL_contactStatus TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_contactStatus result = TL_contactStatus.constructor != constructor ? null : new TL_contactStatus();
            return TLdeserialize(TL_contactStatus.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            user_id = stream.readInt64(exception);
            status = UserStatus.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(user_id);
            status.serializeToStream(stream);
        }
    }

    public static abstract class GroupCall extends TLObject {

        public int flags;
        public boolean join_muted;
        public boolean can_change_join_muted;
        public boolean join_date_asc;
        public boolean schedule_start_subscribed;
        public boolean can_start_video;
        public boolean record_video_active;
        public boolean conference;
        public boolean rtmp_stream;
        public boolean listeners_hidden;
        public boolean creator;
        public long id;
        public long access_hash;
        public int participants_count;
        public String title;
        public int stream_dc_id;
        public int record_start_date;
        public int schedule_date;
        public int unmuted_video_count;
        public int unmuted_video_limit;
        public int version;
        public int duration;
        public long conference_from_call;
        public String invite_link;
        public boolean messages_enabled;
        public boolean can_change_messages_enabled;
        public boolean min;
        public long send_paid_messages_stars;
        public TLRPC.Peer default_send_as;

        public static GroupCall TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            return TLdeserialize(GroupCall.class, fromConstructor(constructor), stream, constructor, exception);
        }

        private static GroupCall fromConstructor(int constructor) {
            switch (constructor) {
                case TL_groupCallDiscarded.constructor:     return new TL_groupCallDiscarded();
                case TL_groupCall.constructor:              return new TL_groupCall();
                case TL_groupCall_layer216.constructor:     return new TL_groupCall_layer216();
                case TL_groupCall_layer201_2.constructor:   return new TL_groupCall_layer201_2();
                case TL_groupCall_layer201.constructor:     return new TL_groupCall_layer201();
            }
            return null;
        }
    }

    public static class TL_groupCallDiscarded extends GroupCall {
        public static final int constructor = 0x7780bcb4;

        public void readParams(InputSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
            access_hash = stream.readInt64(exception);
            duration = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(id);
            stream.writeInt64(access_hash);
            stream.writeInt32(duration);
        }
    }

    public static class TL_groupCall extends GroupCall {
        public static final int constructor = 0xefb2b617;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            join_muted = hasFlag(flags, FLAG_1);
            can_change_join_muted = hasFlag(flags, FLAG_2);
            join_date_asc = hasFlag(flags, FLAG_6);
            schedule_start_subscribed = hasFlag(flags, FLAG_8);
            can_start_video = hasFlag(flags, FLAG_9);
            record_video_active = hasFlag(flags, FLAG_11);
            rtmp_stream = hasFlag(flags, FLAG_12);
            listeners_hidden = hasFlag(flags, FLAG_13);
            conference = hasFlag(flags, FLAG_14);
            creator = hasFlag(flags, FLAG_15);
            messages_enabled = hasFlag(flags, FLAG_17);
            can_change_messages_enabled = hasFlag(flags, FLAG_18);
            min = hasFlag(flags, FLAG_19);
            id = stream.readInt64(exception);
            access_hash = stream.readInt64(exception);
            participants_count = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_3)) {
                title = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                stream_dc_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_5)) {
                record_start_date = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_7)) {
                schedule_date = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_10)) {
                unmuted_video_count = stream.readInt32(exception);
            }
            unmuted_video_limit = stream.readInt32(exception);
            version = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_16)) {
                invite_link = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_20)) {
                send_paid_messages_stars = stream.readInt64(exception);
            }
            if (hasFlag(flags, FLAG_21)) {
                default_send_as = TLRPC.Peer.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_1, join_muted);
            flags = setFlag(flags, FLAG_2, can_change_join_muted);
            flags = setFlag(flags, FLAG_6, join_date_asc);
            flags = setFlag(flags, FLAG_8, schedule_start_subscribed);
            flags = setFlag(flags, FLAG_9, can_start_video);
            flags = setFlag(flags, FLAG_11, record_video_active);
            flags = setFlag(flags, FLAG_12, rtmp_stream);
            flags = setFlag(flags, FLAG_13, listeners_hidden);
            flags = setFlag(flags, FLAG_14, conference);
            flags = setFlag(flags, FLAG_15, creator);
            flags = setFlag(flags, FLAG_17, messages_enabled);
            flags = setFlag(flags, FLAG_18, can_change_messages_enabled);
            flags = setFlag(flags, FLAG_19, min);
            stream.writeInt32(flags);
            stream.writeInt64(id);
            stream.writeInt64(access_hash);
            stream.writeInt32(participants_count);
            if (hasFlag(flags, FLAG_3)) {
                stream.writeString(title);
            }
            if (hasFlag(flags, FLAG_4)) {
                stream.writeInt32(stream_dc_id);
            }
            if (hasFlag(flags, FLAG_5)) {
                stream.writeInt32(record_start_date);
            }
            if (hasFlag(flags, FLAG_7)) {
                stream.writeInt32(schedule_date);
            }
            if (hasFlag(flags, FLAG_10)) {
                stream.writeInt32(unmuted_video_count);
            }
            stream.writeInt32(unmuted_video_limit);
            stream.writeInt32(version);
            if (hasFlag(flags, FLAG_16)) {
                stream.writeString(invite_link);
            }
            if (hasFlag(flags, FLAG_20)) {
                stream.writeInt64(send_paid_messages_stars);
            }
            if (hasFlag(flags, FLAG_21)) {
                default_send_as.serializeToStream(stream);
            }
        }
    }

    public static class TL_groupCall_layer216 extends TL_groupCall {
        public static final int constructor = 0x553b0ba1;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            join_muted = hasFlag(flags, FLAG_1);
            can_change_join_muted = hasFlag(flags, FLAG_2);
            join_date_asc = hasFlag(flags, FLAG_6);
            schedule_start_subscribed = hasFlag(flags, FLAG_8);
            can_start_video = hasFlag(flags, FLAG_9);
            record_video_active = hasFlag(flags, FLAG_11);
            rtmp_stream = hasFlag(flags, FLAG_12);
            listeners_hidden = hasFlag(flags, FLAG_13);
            conference = hasFlag(flags, FLAG_14);
            creator = hasFlag(flags, FLAG_15);
            messages_enabled = hasFlag(flags, FLAG_17);
            can_change_messages_enabled = hasFlag(flags, FLAG_18);
            min = hasFlag(flags, FLAG_19);
            id = stream.readInt64(exception);
            access_hash = stream.readInt64(exception);
            participants_count = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_3)) {
                title = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                stream_dc_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_5)) {
                record_start_date = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_7)) {
                schedule_date = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_10)) {
                unmuted_video_count = stream.readInt32(exception);
            }
            unmuted_video_limit = stream.readInt32(exception);
            version = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_16)) {
                invite_link = stream.readString(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_1, join_muted);
            flags = setFlag(flags, FLAG_2, can_change_join_muted);
            flags = setFlag(flags, FLAG_6, join_date_asc);
            flags = setFlag(flags, FLAG_8, schedule_start_subscribed);
            flags = setFlag(flags, FLAG_9, can_start_video);
            flags = setFlag(flags, FLAG_11, record_video_active);
            flags = setFlag(flags, FLAG_12, rtmp_stream);
            flags = setFlag(flags, FLAG_13, listeners_hidden);
            flags = setFlag(flags, FLAG_14, conference);
            flags = setFlag(flags, FLAG_15, creator);
            flags = setFlag(flags, FLAG_17, messages_enabled);
            flags = setFlag(flags, FLAG_18, can_change_messages_enabled);
            flags = setFlag(flags, FLAG_19, min);
            stream.writeInt32(flags);
            stream.writeInt64(id);
            stream.writeInt64(access_hash);
            stream.writeInt32(participants_count);
            if (hasFlag(flags, FLAG_3)) {
                stream.writeString(title);
            }
            if (hasFlag(flags, FLAG_4)) {
                stream.writeInt32(stream_dc_id);
            }
            if (hasFlag(flags, FLAG_5)) {
                stream.writeInt32(record_start_date);
            }
            if (hasFlag(flags, FLAG_7)) {
                stream.writeInt32(schedule_date);
            }
            if (hasFlag(flags, FLAG_10)) {
                stream.writeInt32(unmuted_video_count);
            }
            stream.writeInt32(unmuted_video_limit);
            stream.writeInt32(version);
            if (hasFlag(flags, FLAG_16)) {
                stream.writeString(invite_link);
            }
        }
    }

    public static class TL_groupCall_layer201_2 extends TL_groupCall {
        public static final int constructor = 0xd597650c;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            join_muted = hasFlag(flags, FLAG_1);
            can_change_join_muted = hasFlag(flags, FLAG_2);
            join_date_asc = hasFlag(flags, FLAG_6);
            schedule_start_subscribed = hasFlag(flags, FLAG_8);
            can_start_video = hasFlag(flags, FLAG_9);
            record_video_active = hasFlag(flags, FLAG_11);
            rtmp_stream = hasFlag(flags, FLAG_12);
            listeners_hidden = hasFlag(flags, FLAG_13);
            conference = hasFlag(flags, FLAG_14);
            creator = hasFlag(flags, FLAG_15);
            id = stream.readInt64(exception);
            access_hash = stream.readInt64(exception);
            participants_count = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_3)) {
                title = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                stream_dc_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_5)) {
                record_start_date = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_7)) {
                schedule_date = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_10)) {
                unmuted_video_count = stream.readInt32(exception);
            }
            unmuted_video_limit = stream.readInt32(exception);
            version = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_16)) {
                invite_link = stream.readString(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_1, join_muted);
            flags = setFlag(flags, FLAG_2, can_change_join_muted);
            flags = setFlag(flags, FLAG_6, join_date_asc);
            flags = setFlag(flags, FLAG_8, schedule_start_subscribed);
            flags = setFlag(flags, FLAG_9, can_start_video);
            flags = setFlag(flags, FLAG_11, record_video_active);
            flags = setFlag(flags, FLAG_12, rtmp_stream);
            flags = setFlag(flags, FLAG_13, listeners_hidden);
            flags = setFlag(flags, FLAG_14, conference);
            flags = setFlag(flags, FLAG_15, creator);
            stream.writeInt32(flags);
            stream.writeInt64(id);
            stream.writeInt64(access_hash);
            stream.writeInt32(participants_count);
            if (hasFlag(flags, FLAG_3)) {
                stream.writeString(title);
            }
            if (hasFlag(flags, FLAG_4)) {
                stream.writeInt32(stream_dc_id);
            }
            if (hasFlag(flags, FLAG_5)) {
                stream.writeInt32(record_start_date);
            }
            if (hasFlag(flags, FLAG_7)) {
                stream.writeInt32(schedule_date);
            }
            if (hasFlag(flags, FLAG_10)) {
                stream.writeInt32(unmuted_video_count);
            }
            stream.writeInt32(unmuted_video_limit);
            stream.writeInt32(version);
            if (hasFlag(flags, FLAG_16)) {
                stream.writeString(invite_link);
            }
        }
    }

    public static class TL_groupCall_layer201 extends TL_groupCall {
        public static final int constructor = 0xcdf8d3e3;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            join_muted = hasFlag(flags, FLAG_1);
            can_change_join_muted = hasFlag(flags, FLAG_2);
            join_date_asc = hasFlag(flags, FLAG_6);
            schedule_start_subscribed = hasFlag(flags, FLAG_8);
            can_start_video = hasFlag(flags, FLAG_9);
            record_video_active = hasFlag(flags, FLAG_11);
            rtmp_stream = hasFlag(flags, FLAG_12);
            listeners_hidden = hasFlag(flags, FLAG_13);
            conference = hasFlag(flags, FLAG_14);
            id = stream.readInt64(exception);
            access_hash = stream.readInt64(exception);
            participants_count = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_3)) {
                title = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                stream_dc_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_5)) {
                record_start_date = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_7)) {
                schedule_date = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_10)) {
                unmuted_video_count = stream.readInt32(exception);
            }
            unmuted_video_limit = stream.readInt32(exception);
            version = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_14)) {
                conference_from_call = stream.readInt64(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_1, join_muted);
            flags = setFlag(flags, FLAG_2, can_change_join_muted);
            flags = setFlag(flags, FLAG_6, join_date_asc);
            flags = setFlag(flags, FLAG_8, schedule_start_subscribed);
            flags = setFlag(flags, FLAG_9, can_start_video);
            flags = setFlag(flags, FLAG_11, record_video_active);
            flags = setFlag(flags, FLAG_12, rtmp_stream);
            flags = setFlag(flags, FLAG_13, listeners_hidden);
            flags = setFlag(flags, FLAG_14, conference);
            stream.writeInt32(flags);
            stream.writeInt64(id);
            stream.writeInt64(access_hash);
            stream.writeInt32(participants_count);
            if (hasFlag(flags, FLAG_3)) {
                stream.writeString(title);
            }
            if (hasFlag(flags, FLAG_4)) {
                stream.writeInt32(stream_dc_id);
            }
            if (hasFlag(flags, FLAG_5)) {
                stream.writeInt32(record_start_date);
            }
            if (hasFlag(flags, FLAG_7)) {
                stream.writeInt32(schedule_date);
            }
            if (hasFlag(flags, FLAG_10)) {
                stream.writeInt32(unmuted_video_count);
            }
            stream.writeInt32(unmuted_video_limit);
            stream.writeInt32(version);
            if (hasFlag(flags, FLAG_14)) {
                stream.writeInt64(conference_from_call);
            }
        }
    }

    public static class TL_channelBannedRights_layer92 extends TLObject {
        public static final int constructor = 0x58cf4249;

        public int flags;
        public boolean view_messages;
        public boolean send_messages;
        public boolean send_media;
        public boolean send_stickers;
        public boolean send_gifs;
        public boolean send_games;
        public boolean send_inline;
        public boolean embed_links;
        public int until_date;

        public static TL_channelBannedRights_layer92 TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_channelBannedRights_layer92 result = TL_channelBannedRights_layer92.constructor != constructor ? null : new TL_channelBannedRights_layer92();
            return TLdeserialize(TL_channelBannedRights_layer92.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            view_messages = hasFlag(flags, FLAG_0);
            send_messages = hasFlag(flags, FLAG_1);
            send_media = hasFlag(flags, FLAG_2);
            send_stickers = hasFlag(flags, FLAG_3);
            send_gifs = hasFlag(flags, FLAG_4);
            send_games = hasFlag(flags, FLAG_5);
            send_inline = hasFlag(flags, FLAG_6);
            embed_links = hasFlag(flags, FLAG_7);
            until_date = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, view_messages);
            flags = setFlag(flags, FLAG_1, send_messages);
            flags = setFlag(flags, FLAG_2, send_media);
            flags = setFlag(flags, FLAG_3, send_stickers);
            flags = setFlag(flags, FLAG_4, send_gifs);
            flags = setFlag(flags, FLAG_5, send_games);
            flags = setFlag(flags, FLAG_6, send_inline);
            flags = setFlag(flags, FLAG_7, embed_links);
            stream.writeInt32(flags);
            stream.writeInt32(until_date);
        }
    }

    public static abstract class DialogPeer extends TLObject {

        public static DialogPeer TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            return TLdeserialize(DialogPeer.class, fromConstructor(constructor), stream, constructor, exception);
        }

        private static DialogPeer fromConstructor(int constructor) {
            switch (constructor) {
                case TL_dialogPeer.constructor:
                    return new TL_dialogPeer();
                case TL_dialogPeerFolder.constructor:
                    return new TL_dialogPeerFolder();
                case TL_dialogPeerCommunity.constructor:
                    return new TL_dialogPeerCommunity();
                default:
                    return null;
            }
        }
    }

    public static class TL_dialogPeer extends DialogPeer {
        public static final int constructor = 0xe56dbf05;

        public Peer peer;

        public void readParams(InputSerializedData stream, boolean exception) {
            peer = Peer.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            peer.serializeToStream(stream);
        }
    }

    public static class TL_dialogPeerFolder extends DialogPeer {
        public static final int constructor = 0x514519e2;

        public int folder_id;

        public void readParams(InputSerializedData stream, boolean exception) {
            folder_id = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(folder_id);
        }
    }

    public static class TL_dialogPeerCommunity extends DialogPeer {
        public static final int constructor = 0x2F65C8E4;

        public long community_id;

        public void readParams(InputSerializedData stream, boolean exception) {
            community_id = stream.readInt64(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(community_id);
        }
    }

    public static abstract class MessagePeerReaction extends TLObject {

        public int flags;
        public boolean big;
        public boolean unread;
        public Peer peer_id;
        public Reaction reaction;
        public int date;
        public boolean dateIsSeen; //custom

        public static MessagePeerReaction TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            MessagePeerReaction result = null;
            switch (constructor) {
                case 0x8c79b63c:
                    result = new TL_messagePeerReaction();
                    break;
                case 0xb156fe9c:
                    result = new TL_messagePeerReaction_layer154();
                    break;
                case 0x51b67eff:
                    result = new TL_messagePeerReaction_layer144();
                    break;
                case 0x932844fa:
                    result = new TL_messagePeerReaction_layer137();
                    break;
            }
            return TLdeserialize(MessagePeerReaction.class, result, stream, constructor, exception);
        }
    }

    public static class TL_messagePeerReaction extends MessagePeerReaction {
        public static final int constructor = 0x8c79b63c;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            big = hasFlag(flags, FLAG_0);
            unread = hasFlag(flags, FLAG_1);
            peer_id = Peer.TLdeserialize(stream, stream.readInt32(exception), exception);
            date = stream.readInt32(exception);
            reaction = Reaction.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, big);
            flags = setFlag(flags, FLAG_1, unread);
            stream.writeInt32(flags);
            peer_id.serializeToStream(stream);
            stream.writeInt32(date);
            reaction.serializeToStream(stream);
        }
    }

    public static class TL_messagePeerReaction_layer154 extends MessagePeerReaction {
        public static final int constructor = 0xb156fe9c;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            big = hasFlag(flags, FLAG_0);
            unread = hasFlag(flags, FLAG_1);
            peer_id = Peer.TLdeserialize(stream, stream.readInt32(exception), exception);
            reaction = Reaction.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, big);
            flags = setFlag(flags, FLAG_1, unread);
            stream.writeInt32(flags);
            peer_id.serializeToStream(stream);
            reaction.serializeToStream(stream);
        }
    }

    public static class TL_messagePeerReaction_layer144 extends MessagePeerReaction {
        public static final int constructor = 0x51b67eff;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            big = hasFlag(flags, FLAG_0);
            unread = hasFlag(flags, FLAG_1);
            peer_id = Peer.TLdeserialize(stream, stream.readInt32(exception), exception);
            reaction = new TL_reactionEmoji();
            ((TL_reactionEmoji) reaction).emoticon = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, big);
            flags = setFlag(flags, FLAG_1, unread);
            stream.writeInt32(flags);
            peer_id.serializeToStream(stream);
            if (reaction instanceof TL_reactionEmoji)
                stream.writeString(((TL_reactionEmoji) reaction).emoticon);
            else
                stream.writeString("");
        }
    }

    public static class TL_messagePeerReaction_layer137 extends MessagePeerReaction {
        public static final int constructor = 0x932844fa;

        public long user_id;

        public void readParams(InputSerializedData stream, boolean exception) {
            user_id = stream.readInt64(exception);
            reaction = new TL_reactionEmoji();
            ((TL_reactionEmoji) reaction).emoticon = stream.readString(exception);
            peer_id = new TL_peerUser();
            peer_id.user_id = user_id;
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(user_id);
            if (reaction instanceof TL_reactionEmoji)
                stream.writeString(((TL_reactionEmoji) reaction).emoticon);
            else
                stream.writeString("");
        }
    }

    public static abstract class auth_Authorization extends TLObject {

        public static auth_Authorization TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            auth_Authorization result = null;
            switch (constructor) {
                case 0x44747e9a:
                    result = new TL_auth_authorizationSignUpRequired();
                    break;
                case 0x33fb7bb8://TODO old constructor need remove
                    result = new TL_auth_authorization();
                    break;
                case 0x2ea2c0d4:
                    result = new TL_auth_authorization();
                    break;
            }
            return TLdeserialize(auth_Authorization.class, result, stream, constructor, exception);
        }
    }

    public static class TL_auth_authorizationSignUpRequired extends auth_Authorization {
        public static final int constructor = 0x44747e9a;

        public int flags;
        public TL_help_termsOfService terms_of_service;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_0)) {
                terms_of_service = TL_help_termsOfService.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_0)) {
                terms_of_service.serializeToStream(stream);
            }
        }
    }

    public static class TL_auth_authorization extends auth_Authorization {
        public static final int constructor = 0x2ea2c0d4;

        public int flags;
        public boolean setup_password_required;
        public int otherwise_relogin_days;
        public int tmp_sessions;
        public byte[] future_auth_token;
        public User user;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            setup_password_required = hasFlag(flags, FLAG_1);
            if (hasFlag(flags, FLAG_1)) {
                otherwise_relogin_days = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_0)) {
                tmp_sessions = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                future_auth_token = stream.readByteArray(exception);
            }
            user = User.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_1, setup_password_required);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_1)) {
                stream.writeInt32(otherwise_relogin_days);
            }
            if (hasFlag(flags, FLAG_0)) {
                stream.writeInt32(tmp_sessions);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeByteArray(future_auth_token);
            }
            user.serializeToStream(stream);
        }
    }

    public static class PollAnswer extends TLObject {
        public int flags;
        public TL_textWithEntities text = new TL_textWithEntities();
        public byte[] option;
        public MessageMedia media;
        public InputMedia input_media;
        public TLRPC.Peer added_by;
        public int date;
        
        public int unshuffled_index; // custom
        public long shuffle_hash; // custom

        private static PollAnswer fromConstructor(int constructor) {
            switch (constructor) {
                case TL_pollAnswer.constructor:
                    return new TL_pollAnswer();
                case TL_pollAnswer_layer223.constructor:
                    return new TL_pollAnswer_layer223();
                case TL_pollAnswer_layer178.constructor:
                    return new TL_pollAnswer_layer178();
                case TL_inputPollAnswer.constructor:
                    return new TL_inputPollAnswer();
            }
            return null;
        }

        public static PollAnswer TLdeserialize(InputSerializedData stream, boolean exception) {
            return TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public static PollAnswer TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            return TLdeserialize(PollAnswer.class, fromConstructor(constructor), stream, constructor, exception);
        }
    }

    public static class TL_inputPollAnswer extends PollAnswer {
        public static final int constructor = 0x199FED96;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            text = TL_textWithEntities.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_0)) {
                input_media = InputMedia.TLdeserialize(stream, exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, input_media != null);
            stream.writeInt32(flags);
            text.serializeToStream(stream);
            if (hasFlag(flags, FLAG_0)) {
                input_media.serializeToStream(stream);
            }
        }
    }

    public static class TL_pollAnswer extends PollAnswer {
        public static final int constructor = 0x4B7D786A;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            text = TL_textWithEntities.TLdeserialize(stream, stream.readInt32(exception), exception);
            option = stream.readByteArray(exception);
            if (hasFlag(flags, FLAG_0)) {
                media = MessageMedia.TLdeserialize(stream, exception);
            }
            if (hasFlag(flags, FLAG_1)) {
                added_by = TLRPC.Peer.TLdeserialize(stream, stream.readInt32(exception), exception);
                date = stream.readInt32(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, media != null);
            flags = setFlag(flags, FLAG_1, added_by != null);
            stream.writeInt32(flags);
            text.serializeToStream(stream);
            stream.writeByteArray(option);
            if (hasFlag(flags, FLAG_0)) {
                media.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_1)) {
                added_by.serializeToStream(stream);
                stream.writeInt32(date);
            }
        }
    }

    public static class TL_pollAnswer_layer223 extends TL_pollAnswer {
        public static final int constructor = 0xff16e2ca;

        public void readParams(InputSerializedData stream, boolean exception) {
            text = TL_textWithEntities.TLdeserialize(stream, stream.readInt32(exception), exception);
            option = stream.readByteArray(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            text.serializeToStream(stream);
            stream.writeByteArray(option);
        }
    }

    public static class TL_pollAnswer_layer178 extends TL_pollAnswer {
        public static final int constructor = 0x6ca9c2e9;

        public void readParams(InputSerializedData stream, boolean exception) {
            text = new TL_textWithEntities();
            text.text = stream.readString(exception);
            option = stream.readByteArray(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(text == null ? "" : text.text);
            stream.writeByteArray(option);
        }
    }

    public static abstract class JSONValue extends TLObject {

        public static JSONValue TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            JSONValue result = null;
            switch (constructor) {
                case 0xc7345e6a:
                    result = new TL_jsonBool();
                    break;
                case 0x3f6d7b68:
                    result = new TL_jsonNull();
                    break;
                case 0xb71e767a:
                    result = new TL_jsonString();
                    break;
                case 0xf7444763:
                    result = new TL_jsonArray();
                    break;
                case 0x99c1d49d:
                    result = new TL_jsonObject();
                    break;
                case 0x2be0dfa4:
                    result = new TL_jsonNumber();
                    break;
            }
            return TLdeserialize(JSONValue.class, result, stream, constructor, exception);
        }
    }

    public static class TL_jsonBool extends JSONValue {
        public static final int constructor = 0xc7345e6a;

        public boolean value;

        public void readParams(InputSerializedData stream, boolean exception) {
            value = stream.readBool(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeBool(value);
        }
    }

    public static class TL_jsonNull extends JSONValue {
        public static final int constructor = 0x3f6d7b68;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_jsonString extends JSONValue {
        public static final int constructor = 0xb71e767a;

        public String value;

        public void readParams(InputSerializedData stream, boolean exception) {
            value = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(value);
        }
    }

    public static class TL_jsonArray extends JSONValue {
        public static final int constructor = 0xf7444763;

        public ArrayList<JSONValue> value = new ArrayList<>();

        public void readParams(InputSerializedData stream, boolean exception) {
            value = Vector.deserialize(stream, JSONValue::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            Vector.serialize(stream, value);
        }
    }

    public static class TL_jsonObject extends JSONValue {
        public static final int constructor = 0x99c1d49d;

        public ArrayList<TL_jsonObjectValue> value = new ArrayList<>();

        public void readParams(InputSerializedData stream, boolean exception) {
            value = Vector.deserialize(stream, TL_jsonObjectValue::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            Vector.serialize(stream, value);
        }
    }

    public static class TL_jsonNumber extends JSONValue {
        public static final int constructor = 0x2be0dfa4;

        public double value;

        public void readParams(InputSerializedData stream, boolean exception) {
            value = stream.readDouble(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeDouble(value);
        }
    }

    public static abstract class InputWallPaper extends TLObject {

        public static InputWallPaper TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            InputWallPaper result = null;
            switch (constructor) {
                case 0xe630b979:
                    result = new TL_inputWallPaper();
                    break;
                case 0x967a462e:
                    result = new TL_inputWallPaperNoFile();
                    break;
                case 0x72091c80:
                    result = new TL_inputWallPaperSlug();
                    break;
            }
            return TLdeserialize(InputWallPaper.class, result, stream, constructor, exception);
        }
    }

    public static class TL_inputWallPaper extends InputWallPaper {
        public static final int constructor = 0xe630b979;

        public long id;
        public long access_hash;

        public void readParams(InputSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
            access_hash = stream.readInt64(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(id);
            stream.writeInt64(access_hash);
        }
    }

    public static class TL_inputWallPaperNoFile extends InputWallPaper {
        public static final int constructor = 0x967a462e;

        public long id;

        public void readParams(InputSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(id);
        }
    }

    public static class TL_inputWallPaperSlug extends InputWallPaper {
        public static final int constructor = 0x72091c80;

        public String slug;

        public void readParams(InputSerializedData stream, boolean exception) {
            slug = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(slug);
        }
    }

    public static class TL_messages_historyImportParsed extends TLObject {
        public static final int constructor = 0x5e0fb7b9;

        public int flags;
        public boolean pm;
        public boolean group;
        public String title;

        public static TL_messages_historyImportParsed TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_messages_historyImportParsed result = TL_messages_historyImportParsed.constructor != constructor ? null : new TL_messages_historyImportParsed();
            return TLdeserialize(TL_messages_historyImportParsed.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            pm = hasFlag(flags, FLAG_0);
            group = hasFlag(flags, FLAG_1);
            if (hasFlag(flags, FLAG_2)) {
                title = stream.readString(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, pm);
            flags = setFlag(flags, FLAG_1, group);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_2)) {
                stream.writeString(title);
            }
        }
    }

    public static class TL_folder extends TLObject {
        public static final int constructor = 0xff544e65;

        public int flags;
        public boolean autofill_new_broadcasts;
        public boolean autofill_public_groups;
        public boolean autofill_new_correspondents;
        public int id;
        public String title;
        public ChatPhoto photo;

        public static TL_folder TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_folder result = TL_folder.constructor != constructor ? null : new TL_folder();
            return TLdeserialize(TL_folder.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            autofill_new_broadcasts = hasFlag(flags, FLAG_0);
            autofill_public_groups = hasFlag(flags, FLAG_1);
            autofill_new_correspondents = hasFlag(flags, FLAG_2);
            id = stream.readInt32(exception);
            title = stream.readString(exception);
            if (hasFlag(flags, FLAG_3)) {
                photo = ChatPhoto.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, autofill_new_broadcasts);
            flags = setFlag(flags, FLAG_1, autofill_public_groups);
            flags = setFlag(flags, FLAG_2, autofill_new_correspondents);
            stream.writeInt32(flags);
            stream.writeInt32(id);
            stream.writeString(title);
            if (hasFlag(flags, FLAG_3)) {
                photo.serializeToStream(stream);
            }
        }
    }

    public static abstract class messages_Messages extends TLObject {
        public ArrayList<Message> messages = new ArrayList<>();
        public ArrayList<Chat> chats = new ArrayList<>();
        public ArrayList<User> users = new ArrayList<>();
        public ArrayList<TL_forumTopic> topics = new ArrayList<>();
        public int flags;
        public boolean inexact;
        public int pts;
        public int count;
        public int next_rate;
        public int offset_id_offset;
        public ArrayList<Document> animatedEmoji;
        public SearchPostsFlood search_flood;

        public static messages_Messages TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            messages_Messages result = null;
            switch (constructor) {
                case TL_messages_messagesSlice.constructor:
                    result = new TL_messages_messagesSlice();
                    break;
                case TL_messages_messagesSlice_layer215.constructor:
                    result = new TL_messages_messagesSlice_layer215();
                    break;
                case TL_messages_messagesSlice_layer210.constructor:
                    result = new TL_messages_messagesSlice_layer210();
                    break;
                case TL_messages_messages.constructor:
                    result = new TL_messages_messages();
                    break;
                case TL_messages_messages_layer215.constructor:
                    result = new TL_messages_messages_layer215();
                    break;
                case TL_messages_channelMessages.constructor:
                    result = new TL_messages_channelMessages();
                    break;
                case TL_messages_messagesNotModified.constructor:
                    result = new TL_messages_messagesNotModified();
                    break;
            }
            return TLdeserialize(messages_Messages.class, result, stream, constructor, exception);
        }
    }

    public static class TL_messages_messagesSlice extends messages_Messages {
        public static final int constructor = 0x5F206716;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            inexact = hasFlag(flags, FLAG_1);
            count = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_0)) {
                next_rate = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                offset_id_offset = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_3)) {
                search_flood = SearchPostsFlood.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            messages = Vector.deserialize(stream, Message::TLdeserialize, exception);
            topics = Vector.deserialize(stream, TL_forumTopic::TLdeserialize, exception);
            chats = Vector.deserialize(stream, Chat::TLdeserialize, exception);
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_1, inexact);
            stream.writeInt32(flags);
            stream.writeInt32(count);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeInt32(next_rate);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(offset_id_offset);
            }
            if (hasFlag(flags, FLAG_3)) {
                search_flood.serializeToStream(stream);
            }
            Vector.serialize(stream, messages);
            Vector.serialize(stream, topics);
            Vector.serialize(stream, chats);
            Vector.serialize(stream, users);
        }
    }

    public static class TL_messages_messagesSlice_layer215 extends  TL_messages_messagesSlice {
        public static final int constructor = 0x762b263d;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            inexact = hasFlag(flags, FLAG_1);
            count = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_0)) {
                next_rate = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                offset_id_offset = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_3)) {
                search_flood = SearchPostsFlood.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            messages = Vector.deserialize(stream, Message::TLdeserialize, exception);
            chats = Vector.deserialize(stream, Chat::TLdeserialize, exception);
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_1, inexact);
            stream.writeInt32(flags);
            stream.writeInt32(count);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeInt32(next_rate);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(offset_id_offset);
            }
            if (hasFlag(flags, FLAG_3)) {
                search_flood.serializeToStream(stream);
            }
            Vector.serialize(stream, messages);
            Vector.serialize(stream, chats);
            Vector.serialize(stream, users);
        }
    }

    public static class TL_messages_messagesSlice_layer210 extends TL_messages_messagesSlice {
        public static final int constructor = 0x3a54685e;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            inexact = hasFlag(flags, FLAG_1);
            count = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_0)) {
                next_rate = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                offset_id_offset = stream.readInt32(exception);
            }
            messages = Vector.deserialize(stream, Message::TLdeserialize, exception);
            chats = Vector.deserialize(stream, Chat::TLdeserialize, exception);
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_1, inexact);
            stream.writeInt32(flags);
            stream.writeInt32(count);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeInt32(next_rate);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(offset_id_offset);
            }
            Vector.serialize(stream, messages);
            Vector.serialize(stream, chats);
            Vector.serialize(stream, users);
        }
    }

    public static class TL_messages_messages extends messages_Messages {
        public static final int constructor = 0x1D73E7EA;

        public void readParams(InputSerializedData stream, boolean exception) {
            messages = Vector.deserialize(stream, Message::TLdeserialize, exception);
            topics = Vector.deserialize(stream, TL_forumTopic::TLdeserialize, exception);
            chats = Vector.deserialize(stream, Chat::TLdeserialize, exception);
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            Vector.serialize(stream, messages);
            Vector.serialize(stream, topics);
            Vector.serialize(stream, chats);
            Vector.serialize(stream, users);
        }
    }

    public static class TL_messages_messages_layer215 extends messages_Messages {
        public static final int constructor = 0x8c718e87;

        public void readParams(InputSerializedData stream, boolean exception) {
            messages = Vector.deserialize(stream, Message::TLdeserialize, exception);
            chats = Vector.deserialize(stream, Chat::TLdeserialize, exception);
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            Vector.serialize(stream, messages);
            Vector.serialize(stream, chats);
            Vector.serialize(stream, users);
        }
    }

    public static class TL_messages_channelMessages extends messages_Messages {
        public static final int constructor = 0xc776ba4e;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            inexact = hasFlag(flags, FLAG_1);
            pts = stream.readInt32(exception);
            count = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_2)) {
                offset_id_offset = stream.readInt32(exception);
            }
            messages = Vector.deserialize(stream, Message::TLdeserialize, exception);
            topics = Vector.deserialize(stream, TL_forumTopic::TLdeserialize, exception);
            chats = Vector.deserialize(stream, Chat::TLdeserialize, exception);
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_1, inexact);
            stream.writeInt32(flags);
            stream.writeInt32(pts);
            stream.writeInt32(count);
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(offset_id_offset);
            }
            Vector.serialize(stream, messages);
            Vector.serialize(stream, topics);
            Vector.serialize(stream, chats);
            Vector.serialize(stream, users);
        }
    }

    public static class TL_messages_messagesNotModified extends messages_Messages {
        public static final int constructor = 0x74535f21;

        public void readParams(InputSerializedData stream, boolean exception) {
            count = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(count);
        }
    }

    public static class PaymentForm extends TLObject {
        public int flags;
        public boolean can_save_credentials;
        public boolean password_missing;
        public long form_id;
        public long bot_id;
        public String title;
        public String description;
        public WebDocument photo;
        public TL_invoice invoice;
        public long provider_id;
        public String url;
        public String native_provider;
        public TL_dataJSON native_params;
        public ArrayList<TL_paymentFormMethod> additional_methods = new ArrayList<>();
        public TL_paymentRequestedInfo saved_info;
        public ArrayList<TL_paymentSavedCredentialsCard> saved_credentials = new ArrayList<>();
        public ArrayList<User> users = new ArrayList<>();

        public static PaymentForm TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            PaymentForm result = null;
            switch (constructor) {
                case TL_payments_paymentForm.constructor:
                    result = new TL_payments_paymentForm();
                    break;
                case TL_payments_paymentFormStars.constructor:
                    result = new TL_payments_paymentFormStars();
                    break;
                case TL_payments_paymentFormStarGift.constructor:
                    result = new TL_payments_paymentFormStarGift();
                    break;
            }
            return TLdeserialize(PaymentForm.class, result, stream, constructor, exception);
        }
    }

    public static class TL_payments_paymentForm extends PaymentForm {
        public static final int constructor = 0xa0058751;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            can_save_credentials = hasFlag(flags, FLAG_2);
            password_missing = hasFlag(flags, FLAG_3);
            form_id = stream.readInt64(exception);
            bot_id = stream.readInt64(exception);
            title = stream.readString(exception);
            description = stream.readString(exception);
            if (hasFlag(flags, FLAG_5)) {
                photo = WebDocument.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            invoice = TL_invoice.TLdeserialize(stream, stream.readInt32(exception), exception);
            provider_id = stream.readInt64(exception);
            url = stream.readString(exception);
            if (hasFlag(flags, FLAG_4)) {
                native_provider = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                native_params = TL_dataJSON.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_6)) {
                additional_methods = Vector.deserialize(stream, TL_paymentFormMethod::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_0)) {
                saved_info = TL_paymentRequestedInfo.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_1)) {
                saved_credentials = Vector.deserialize(stream, TL_paymentSavedCredentialsCard::TLdeserialize, exception);
            }
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_2, can_save_credentials);
            flags = setFlag(flags, FLAG_3, password_missing);
            stream.writeInt32(flags);
            stream.writeInt64(form_id);
            stream.writeInt64(bot_id);
            stream.writeString(title);
            stream.writeString(description);
            if (hasFlag(flags, FLAG_5)) {
                photo.serializeToStream(stream);
            }
            invoice.serializeToStream(stream);
            stream.writeInt64(provider_id);
            stream.writeString(url);
            if (hasFlag(flags, FLAG_4)) {
                stream.writeString(native_provider);
            }
            if (hasFlag(flags, FLAG_4)) {
                native_params.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_6)) {
                Vector.serialize(stream, additional_methods);
            }
            if (hasFlag(flags, FLAG_0)) {
                saved_info.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_1)) {
                Vector.serialize(stream, saved_credentials);
            }
            Vector.serialize(stream, users);
        }
    }

    public static class TL_payments_paymentFormStars extends PaymentForm {
        public static final int constructor = 0x7bf6b15c;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            form_id = stream.readInt64(exception);
            bot_id = stream.readInt64(exception);
            title = stream.readString(exception);
            description = stream.readString(exception);
            if (hasFlag(flags, FLAG_5)) {
                photo = WebDocument.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            invoice = TL_invoice.TLdeserialize(stream, stream.readInt32(exception), exception);
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_2, can_save_credentials);
            flags = setFlag(flags, FLAG_3, password_missing);
            stream.writeInt32(flags);
            stream.writeInt64(form_id);
            stream.writeInt64(bot_id);
            stream.writeString(title);
            stream.writeString(description);
            if (hasFlag(flags, FLAG_5)) {
                photo.serializeToStream(stream);
            }
            invoice.serializeToStream(stream);
            Vector.serialize(stream, users);
        }
    }

    public static class TL_payments_paymentFormStarGift extends PaymentForm {
        public static final int constructor = 0xb425cfe1;

        public void readParams(InputSerializedData stream, boolean exception) {
            form_id = stream.readInt64(exception);
            invoice = TL_invoice.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(form_id);
            invoice.serializeToStream(stream);
        }
    }

    public static class TL_paymentFormMethod extends TLObject {
        public static final int constructor = 0x88f8f21b;

        public String url;
        public String title;

        public static TL_paymentFormMethod TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_paymentFormMethod result = TL_paymentFormMethod.constructor != constructor ? null : new TL_paymentFormMethod();
            return TLdeserialize(TL_paymentFormMethod.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            url = stream.readString(exception);
            title = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(url);
            stream.writeString(title);
        }
    }

    public static abstract class ContactLink_layer101 extends TLObject {

        public static ContactLink_layer101 TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            ContactLink_layer101 result = null;
            switch (constructor) {
                case 0xfeedd3ad:
                    result = new TL_contactLinkNone();
                    break;
                case 0xd502c2d0:
                    result = new TL_contactLinkContact();
                    break;
                case 0x5f4f9247:
                    result = new TL_contactLinkUnknown();
                    break;
            }
            return TLdeserialize(ContactLink_layer101.class, result, stream, constructor, exception);
        }
    }

    public static class TL_contactLinkNone extends ContactLink_layer101 {
        public static final int constructor = 0xfeedd3ad;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_contactLinkContact extends ContactLink_layer101 {
        public static final int constructor = 0xd502c2d0;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_contactLinkUnknown extends ContactLink_layer101 {
        public static final int constructor = 0x5f4f9247;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_contacts_link_layer101 extends TLObject {
        public static final int constructor = 0x3ace484c;

        public ContactLink_layer101 my_link;
        public ContactLink_layer101 foreign_link;
        public User user;

        public static TL_contacts_link_layer101 TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_contacts_link_layer101 result = TL_contacts_link_layer101.constructor != constructor ? null : new TL_contacts_link_layer101();
            return TLdeserialize(TL_contacts_link_layer101.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            my_link = ContactLink_layer101.TLdeserialize(stream, stream.readInt32(exception), exception);
            foreign_link = ContactLink_layer101.TLdeserialize(stream, stream.readInt32(exception), exception);
            user = User.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            my_link.serializeToStream(stream);
            foreign_link.serializeToStream(stream);
            user.serializeToStream(stream);
        }
    }

    public static abstract class EncryptedFile extends TLObject {
        public long id;
        public long access_hash;
        public long size;
        public int dc_id;
        public int key_fingerprint;

        public static EncryptedFile TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            EncryptedFile result = null;
            switch (constructor) {
                case 0xa8008cd8:
                    result = new TL_encryptedFile();
                    break;
                case 0x4a70994c:
                    result = new TL_encryptedFile_layer142();
                    break;
                case 0xc21f497e:
                    result = new TL_encryptedFileEmpty();
                    break;
            }
            return TLdeserialize(EncryptedFile.class, result, stream, constructor, exception);
        }
    }

    public static class TL_encryptedFile extends EncryptedFile {
        public static final int constructor = 0xa8008cd8;

        public void readParams(InputSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
            access_hash = stream.readInt64(exception);
            size = stream.readInt64(exception);
            dc_id = stream.readInt32(exception);
            key_fingerprint = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(id);
            stream.writeInt64(access_hash);
            stream.writeInt64(size);
            stream.writeInt32(dc_id);
            stream.writeInt32(key_fingerprint);
        }
    }

    public static class TL_encryptedFile_layer142 extends EncryptedFile {
        public static final int constructor = 0x4a70994c;

        public void readParams(InputSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
            access_hash = stream.readInt64(exception);
            size = stream.readInt32(exception);
            dc_id = stream.readInt32(exception);
            key_fingerprint = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(id);
            stream.writeInt64(access_hash);
            stream.writeInt32((int) size);
            stream.writeInt32(dc_id);
            stream.writeInt32(key_fingerprint);
        }
    }

    public static class TL_encryptedFileEmpty extends EncryptedFile {
        public static final int constructor = 0xc21f497e;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static abstract class Peer extends TLObject {

        public long user_id;
        public long chat_id;
        public long channel_id;

        public static Peer TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            Peer result = null;
            switch (constructor) {
                case 0xa2a5371e:
                    result = new TL_peerChannel();
                    break;
                case 0xbddde532:
                    result = new TL_peerChannel_layer131();
                    break;
                case 0x59511722:
                    result = new TL_peerUser();
                    break;
                case 0x9db1bc6d:
                    result = new TL_peerUser_layer131();
                    break;
                case 0x36c6019a:
                    result = new TL_peerChat();
                    break;
                case 0xbad0e5bb:
                    result = new TL_peerChat_layer131();
                    break;
            }
            return TLdeserialize(Peer.class, result, stream, constructor, exception);
        }
    }

    public static class TL_peerChannel_layer131 extends TL_peerChannel {
        public static final int constructor = 0xbddde532;

        public void readParams(InputSerializedData stream, boolean exception) {
            channel_id = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32((int) channel_id);
        }
    }

    public static class TL_peerUser extends Peer {
        public static final int constructor = 0x59511722;

        public void readParams(InputSerializedData stream, boolean exception) {
            user_id = stream.readInt64(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(user_id);
        }
    }

    public static class TL_peerChannel extends Peer {
        public static final int constructor = 0xa2a5371e;

        public void readParams(InputSerializedData stream, boolean exception) {
            channel_id = stream.readInt64(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(channel_id);
        }
    }

    public static class TL_peerChat extends Peer {
        public static final int constructor = 0x36c6019a;

        public void readParams(InputSerializedData stream, boolean exception) {
            chat_id = stream.readInt64(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(chat_id);
        }
    }

    public static class TL_peerUser_layer131 extends TL_peerUser {
        public static final int constructor = 0x9db1bc6d;

        public void readParams(InputSerializedData stream, boolean exception) {
            user_id = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32((int) user_id);
        }
    }

    public static class TL_peerChat_layer131 extends TL_peerChat {
        public static final int constructor = 0xbad0e5bb;

        public void readParams(InputSerializedData stream, boolean exception) {
            chat_id = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32((int) chat_id);
        }
    }

    public static class TL_labeledPrice extends TLObject {
        public static final int constructor = 0xcb296bf8;

        public String label;
        public long amount;

        public static TL_labeledPrice TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_labeledPrice result = TL_labeledPrice.constructor != constructor ? null : new TL_labeledPrice();
            return TLdeserialize(TL_labeledPrice.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            label = stream.readString(exception);
            amount = stream.readInt64(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(label);
            stream.writeInt64(amount);
        }
    }

    public static class TL_messages_exportedChatInvites extends TLObject {
        public static final int constructor = 0xbdc62dcc;

        public int count;
        public ArrayList<ExportedChatInvite> invites = new ArrayList<>();
        public ArrayList<User> users = new ArrayList<>();

        public static TL_messages_exportedChatInvites TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_messages_exportedChatInvites result = TL_messages_exportedChatInvites.constructor != constructor ? null : new TL_messages_exportedChatInvites();
            return TLdeserialize(TL_messages_exportedChatInvites.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            count = stream.readInt32(exception);
            invites = Vector.deserialize(stream, ExportedChatInvite::TLdeserialize, exception);
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(count);
            Vector.serialize(stream, invites);
            Vector.serialize(stream, users);
        }
    }

    public static class TL_inputStickerSetItem extends TLObject {
        public static final int constructor = 0xffa0a496;

        public int flags;
        public InputDocument document;
        public String emoji;
        public TL_maskCoords mask_coords;

        public static TL_inputStickerSetItem TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_inputStickerSetItem result = TL_inputStickerSetItem.constructor != constructor ? null : new TL_inputStickerSetItem();
            return TLdeserialize(TL_inputStickerSetItem.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            document = InputDocument.TLdeserialize(stream, stream.readInt32(exception), exception);
            emoji = stream.readString(exception);
            if (hasFlag(flags, FLAG_0)) {
                mask_coords = TL_maskCoords.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            document.serializeToStream(stream);
            stream.writeString(emoji);
            if (hasFlag(flags, FLAG_0)) {
                mask_coords.serializeToStream(stream);
            }
        }
    }

    public static class TL_langPackDifference extends TLObject {
        public static final int constructor = 0xf385c1f6;

        public String lang_code;
        public int from_version;
        public int version;
        public ArrayList<LangPackString> strings = new ArrayList<>();

        public static TL_langPackDifference TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_langPackDifference result = TL_langPackDifference.constructor != constructor ? null : new TL_langPackDifference();
            return TLdeserialize(TL_langPackDifference.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            lang_code = stream.readString(exception);
            from_version = stream.readInt32(exception);
            version = stream.readInt32(exception);
            strings = Vector.deserialize(stream, LangPackString::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(lang_code);
            stream.writeInt32(from_version);
            stream.writeInt32(version);
            Vector.serialize(stream, strings);
        }
    }

    public static abstract class help_DeepLinkInfo extends TLObject {

        public static help_DeepLinkInfo TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            help_DeepLinkInfo result = null;
            switch (constructor) {
                case 0x66afa166:
                    result = new TL_help_deepLinkInfoEmpty();
                    break;
                case 0x6a4ee832:
                    result = new TL_help_deepLinkInfo();
                    break;
            }
            return TLdeserialize(help_DeepLinkInfo.class, result, stream, constructor, exception);
        }
    }

    public static class TL_help_deepLinkInfoEmpty extends help_DeepLinkInfo {
        public static final int constructor = 0x66afa166;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_help_deepLinkInfo extends help_DeepLinkInfo {
        public static final int constructor = 0x6a4ee832;

        public int flags;
        public boolean update_app;
        public String message;
        public ArrayList<MessageEntity> entities = new ArrayList<>();

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            update_app = hasFlag(flags, FLAG_0);
            message = stream.readString(exception);
            if (hasFlag(flags, FLAG_1)) {
                entities = Vector.deserialize(stream, MessageEntity::TLdeserialize, exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, update_app);
            stream.writeInt32(flags);
            stream.writeString(message);
            if (hasFlag(flags, FLAG_1)) {
                Vector.serialize(stream, entities);
            }
        }
    }

    public static class TL_chatAdminRights extends TLObject {
        public static final int constructor = 0x5fb224d5;

        public int flags;
        public boolean change_info;
        public boolean post_messages;
        public boolean edit_messages;
        public boolean delete_messages;
        public boolean ban_users;
        public boolean invite_users;
        public boolean pin_messages;
        public boolean add_admins;
        public boolean anonymous;
        public boolean manage_call;
        public boolean other;
        public boolean manage_topics;
        public boolean post_stories;
        public boolean edit_stories;
        public boolean delete_stories;
        public boolean manage_direct_messages;
        public boolean manage_ranks;
        public boolean manage_linked_peers;
        public boolean manage_welcome_messages;

        public static TL_chatAdminRights TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_chatAdminRights result = TL_chatAdminRights.constructor != constructor ? null : new TL_chatAdminRights();
            return TLdeserialize(TL_chatAdminRights.class, result, stream, constructor, exception);
        }

        public static TL_chatAdminRights clone(TL_chatAdminRights rights) {
            if (rights == null) return null;
            SerializedData data = new SerializedData(rights.getObjectSize());
            rights.serializeToStream(data);
            data = new SerializedData(data.toByteArray());
            return TLdeserialize(data, data.readInt32(false), false);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (!(obj instanceof TL_chatAdminRights)) return false;
            return this.flags == ((TL_chatAdminRights) obj).flags;
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            change_info = hasFlag(flags, FLAG_0);
            post_messages = hasFlag(flags, FLAG_1);
            edit_messages = hasFlag(flags, FLAG_2);
            delete_messages = hasFlag(flags, FLAG_3);
            ban_users = hasFlag(flags, FLAG_4);
            invite_users = hasFlag(flags, FLAG_5);
            pin_messages = hasFlag(flags, FLAG_7);
            add_admins = hasFlag(flags, FLAG_9);
            anonymous = hasFlag(flags, FLAG_10);
            manage_call = hasFlag(flags, FLAG_11);
            other = hasFlag(flags, FLAG_12);
            manage_topics = hasFlag(flags, FLAG_13);
            post_stories = hasFlag(flags, FLAG_14);
            edit_stories = hasFlag(flags, FLAG_15);
            delete_stories = hasFlag(flags, FLAG_16);
            manage_direct_messages = hasFlag(flags, FLAG_17);
            manage_ranks = hasFlag(flags, FLAG_18);
            manage_linked_peers = hasFlag(flags, FLAG_19);
            manage_welcome_messages = hasFlag(flags, FLAG_20);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, change_info);
            flags = setFlag(flags, FLAG_1, post_messages);
            flags = setFlag(flags, FLAG_2, edit_messages);
            flags = setFlag(flags, FLAG_3, delete_messages);
            flags = setFlag(flags, FLAG_4, ban_users);
            flags = setFlag(flags, FLAG_5, invite_users);
            flags = setFlag(flags, FLAG_7, pin_messages);
            flags = setFlag(flags, FLAG_9, add_admins);
            flags = setFlag(flags, FLAG_10, anonymous);
            flags = setFlag(flags, FLAG_11, manage_call);
            flags = setFlag(flags, FLAG_12, other);
            flags = setFlag(flags, FLAG_13, manage_topics);
            flags = setFlag(flags, FLAG_14, post_stories);
            flags = setFlag(flags, FLAG_15, edit_stories);
            flags = setFlag(flags, FLAG_16, delete_stories);
            flags = setFlag(flags, FLAG_17, manage_direct_messages);
            flags = setFlag(flags, FLAG_18, manage_ranks);
            flags = setFlag(flags, FLAG_19, manage_linked_peers);
            flags = setFlag(flags, FLAG_20, manage_welcome_messages);
            stream.writeInt32(flags);
        }
    }

    public static abstract class PollResults extends TLObject {

        public int flags;
        public boolean min;
        public ArrayList<PollAnswerVoters> results = new ArrayList<>();
        public int total_voters;
        public ArrayList<Peer> recent_voters = new ArrayList<>();
        public String solution;
        public ArrayList<MessageEntity> solution_entities = new ArrayList<>();
        public MessageMedia solution_media;
        public boolean has_unread_votes;
        public boolean can_view_stats;

        private static PollResults fromConstructor(int constructor) {
            switch (constructor) {
                case TL_pollResults_layer108.constructor:
                    return new TL_pollResults_layer108();
                case TL_pollResults_layer111.constructor:
                    return new TL_pollResults_layer111();
                case TL_pollResults_layer131.constructor:
                    return new TL_pollResults_layer131();
                case TL_pollResults_layer158.constructor:
                    return new TL_pollResults_layer158();
                case TL_pollResults_layer223.constructor:
                    return new TL_pollResults_layer223();
                case TL_pollResults.constructor:
                    return new TL_pollResults();
            }
            return null;
        }

        public static PollResults TLdeserialize(InputSerializedData stream, boolean exception) {
            return TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public static PollResults TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            return TLdeserialize(PollResults.class, fromConstructor(constructor), stream, constructor, exception);
        }
    }

    public static class TL_pollResults_layer108 extends TL_pollResults {
        public static final int constructor = 0x5755785a;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            min = hasFlag(flags, FLAG_0);
            if (hasFlag(flags, FLAG_1)) {
                results = Vector.deserialize(stream, PollAnswerVoters::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                total_voters = stream.readInt32(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, min);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_1)) {
                Vector.serialize(stream, results);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(total_voters);
            }
        }
    }

    public static class TL_pollResults_layer111 extends PollResults {
        public static final int constructor = 0xc87024a2;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            min = hasFlag(flags, FLAG_0);
            if (hasFlag(flags, FLAG_1)) {
                results = Vector.deserialize(stream, PollAnswerVoters::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                total_voters = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_3)) {
                recent_voters = VectorLegacy.deserialize_IntUserIdAsPeer(stream, exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, min);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_1)) {
                Vector.serialize(stream, results);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(total_voters);
            }
            if (hasFlag(flags, FLAG_3)) {
                VectorLegacy.serialize_PeerAsIntUserId(stream, recent_voters);
            }
        }
    }

    public static class TL_pollResults_layer131 extends TL_pollResults {
        public static final int constructor = 0xbadcc1a3;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            min = hasFlag(flags, FLAG_0);
            if (hasFlag(flags, FLAG_1)) {
                results = Vector.deserialize(stream, PollAnswerVoters::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                total_voters = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_3)) {
                recent_voters = VectorLegacy.deserialize_IntUserIdAsPeer(stream, exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                solution = stream.readString(exception);
                solution_entities = Vector.deserialize(stream, MessageEntity::TLdeserialize, exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, min);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_1)) {
                Vector.serialize(stream, results);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(total_voters);
            }
            if (hasFlag(flags, FLAG_3)) {
                VectorLegacy.serialize_PeerAsIntUserId(stream, recent_voters);
            }
            if (hasFlag(flags, FLAG_4)) {
                stream.writeString(solution);
                Vector.serialize(stream, solution_entities);
            }
        }
    }

    public static class TL_pollResults extends PollResults {
        public static final int constructor = 0xba7bb15e;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            min = hasFlag(flags, FLAG_0);
            has_unread_votes = hasFlag(flags, FLAG_6);
            can_view_stats = hasFlag(flags, FLAG_7);
            if (hasFlag(flags, FLAG_1)) {
                results = Vector.deserialize(stream, PollAnswerVoters::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                total_voters = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_3)) {
                recent_voters = Vector.deserialize(stream, Peer::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                solution = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                solution_entities = Vector.deserialize(stream, MessageEntity::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_5)) {
                solution_media = MessageMedia.TLdeserialize(stream, exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, min);
            flags = setFlag(flags, FLAG_5, solution_media != null);
            flags = setFlag(flags, FLAG_6, has_unread_votes);
            flags = setFlag(flags, FLAG_7, can_view_stats);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_1)) {
                Vector.serialize(stream, results);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(total_voters);
            }
            if (hasFlag(flags, FLAG_3)) {
                Vector.serialize(stream, recent_voters);
            }
            if (hasFlag(flags, FLAG_4)) {
                stream.writeString(solution);
            }
            if (hasFlag(flags, FLAG_4)) {
                Vector.serialize(stream, solution_entities);
            }
            if (hasFlag(flags, FLAG_5)) {
                solution_media.serializeToStream(stream);
            }
        }
    }

    public static class TL_pollResults_layer223 extends TL_pollResults {
        public static final int constructor = 0x7adf2420;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            min = hasFlag(flags, FLAG_0);
            if (hasFlag(flags, FLAG_1)) {
                results = Vector.deserialize(stream, PollAnswerVoters::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                total_voters = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_3)) {
                recent_voters = Vector.deserialize(stream, Peer::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                solution = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                solution_entities = Vector.deserialize(stream, MessageEntity::TLdeserialize, exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, min);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_1)) {
                Vector.serialize(stream, results);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(total_voters);
            }
            if (hasFlag(flags, FLAG_3)) {
                Vector.serialize(stream, recent_voters);
            }
            if (hasFlag(flags, FLAG_4)) {
                stream.writeString(solution);
            }
            if (hasFlag(flags, FLAG_4)) {
                Vector.serialize(stream, solution_entities);
            }
        }
    }

    public static class TL_pollResults_layer158 extends PollResults {
        public static final int constructor = 0xdcb82ea3;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            min = hasFlag(flags, FLAG_0);
            if (hasFlag(flags, FLAG_1)) {
                results = Vector.deserialize(stream, PollAnswerVoters::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                total_voters = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_3)) {
                recent_voters = VectorLegacy.deserialize_LongUserIdAsPeer(stream, exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                solution = stream.readString(exception);
                solution_entities = Vector.deserialize(stream, MessageEntity::TLdeserialize, exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, min);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_1)) {
                Vector.serialize(stream, results);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(total_voters);
            }
            if (hasFlag(flags, FLAG_3)) {
                VectorLegacy.serialize_PeerAsLongUserId(stream, recent_voters);
            }
            if (hasFlag(flags, FLAG_4)) {
                stream.writeString(solution);
                Vector.serialize(stream, solution_entities);
            }
        }
    }

    public static abstract class SecureFile extends TLObject {

        public static SecureFile TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            SecureFile result = null;
            switch (constructor) {
                case 0x64199744:
                    result = new TL_secureFileEmpty();
                    break;
                case 0x7d09c27e:
                    result = new TL_secureFile();
                    break;
            }
            return TLdeserialize(SecureFile.class, result, stream, constructor, exception);
        }
    }

    public static class TL_secureFileEmpty extends SecureFile {
        public static final int constructor = 0x64199744;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_secureFile extends SecureFile {
        public static final int constructor = 0x7d09c27e;

        public long id;
        public long access_hash;
        public long size;
        public int dc_id;
        public int date;
        public byte[] file_hash;
        public byte[] secret;

        public void readParams(InputSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
            access_hash = stream.readInt64(exception);
            size = stream.readInt64(exception);
            dc_id = stream.readInt32(exception);
            date = stream.readInt32(exception);
            file_hash = stream.readByteArray(exception);
            secret = stream.readByteArray(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(id);
            stream.writeInt64(access_hash);
            stream.writeInt64(size);
            stream.writeInt32(dc_id);
            stream.writeInt32(date);
            stream.writeByteArray(file_hash);
            stream.writeByteArray(secret);
        }
    }

    public static class TL_secureFile_layer142 extends TL_secureFile {
        public static final int constructor = 0xe0277a62;

        public void readParams(InputSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
            access_hash = stream.readInt64(exception);
            size = stream.readInt32(exception);
            dc_id = stream.readInt32(exception);
            date = stream.readInt32(exception);
            file_hash = stream.readByteArray(exception);
            secret = stream.readByteArray(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(id);
            stream.writeInt64(access_hash);
            stream.writeInt32((int) size);
            stream.writeInt32(dc_id);
            stream.writeInt32(date);
            stream.writeByteArray(file_hash);
            stream.writeByteArray(secret);
        }
    }

    public static class TL_messages_affectedMessages extends TLObject {
        public static final int constructor = 0x84d19185;

        public int pts;
        public int pts_count;

        public static TL_messages_affectedMessages TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_messages_affectedMessages result = TL_messages_affectedMessages.constructor != constructor ? null : new TL_messages_affectedMessages();
            return TLdeserialize(TL_messages_affectedMessages.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            pts = stream.readInt32(exception);
            pts_count = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(pts);
            stream.writeInt32(pts_count);
        }
    }

    public static class TL_messages_chatInviteImporters extends TLObject {
        public static final int constructor = 0x81b6b00a;

        public int count;
        public ArrayList<TL_chatInviteImporter> importers = new ArrayList<>();
        public ArrayList<User> users = new ArrayList<>();

        public static TL_messages_chatInviteImporters TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_messages_chatInviteImporters result = TL_messages_chatInviteImporters.constructor != constructor ? null : new TL_messages_chatInviteImporters();
            return TLdeserialize(TL_messages_chatInviteImporters.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            count = stream.readInt32(exception);
            importers = Vector.deserialize(stream, TL_chatInviteImporter::TLdeserialize, exception);
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(count);
            Vector.serialize(stream, importers);
            Vector.serialize(stream, users);
        }
    }

    public static class PollAnswerVoters extends TLObject {
        public int flags;
        public boolean chosen;
        public boolean correct;
        public byte[] option;
        public int voters;
        public @Nullable ArrayList<Peer> recent_voters;

        private static PollAnswerVoters fromConstructor(int constructor) {
            switch (constructor) {
                case TL_pollAnswerVoters.constructor:
                    return new TL_pollAnswerVoters();
                case TL_pollAnswerVoters_layer223.constructor:
                    return new TL_pollAnswerVoters_layer223();
            }
            return null;
        }

        public static PollAnswerVoters TLdeserialize(InputSerializedData stream, boolean exception) {
            return TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public static PollAnswerVoters TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            return TLdeserialize(PollAnswerVoters.class, fromConstructor(constructor), stream, constructor, exception);
        }
    }

    public static class TL_pollAnswerVoters extends PollAnswerVoters {
        public static final int constructor = 0x3645230a;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            chosen = hasFlag(flags, FLAG_0);
            correct = hasFlag(flags, FLAG_1);
            option = stream.readByteArray(exception);
            if (hasFlag(flags, FLAG_2)) {
                voters = stream.readInt32(exception);
                recent_voters = Vector.deserialize(stream, Peer::TLdeserialize, exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, chosen);
            flags = setFlag(flags, FLAG_1, correct);
            stream.writeInt32(flags);
            stream.writeByteArray(option);
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(voters);
                Vector.serialize(stream, recent_voters);
            }
        }
    }

    public static class TL_pollAnswerVoters_layer223 extends TL_pollAnswerVoters {
        public static final int constructor = 0x3b6ddad2;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            chosen = hasFlag(flags, FLAG_0);
            correct = hasFlag(flags, FLAG_1);
            option = stream.readByteArray(exception);
            voters = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, chosen);
            flags = setFlag(flags, FLAG_1, correct);
            stream.writeInt32(flags);
            stream.writeByteArray(option);
            stream.writeInt32(voters);
        }
    }

    public static class TL_channels_channelParticipant extends TLObject {
        public static final int constructor = 0xdfb80317;

        public ChannelParticipant participant;
        public ArrayList<Chat> chats = new ArrayList<>();
        public ArrayList<User> users = new ArrayList<>();

        public static TL_channels_channelParticipant TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_channels_channelParticipant result = TL_channels_channelParticipant.constructor != constructor ? null : new TL_channels_channelParticipant();
            return TLdeserialize(TL_channels_channelParticipant.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            participant = ChannelParticipant.TLdeserialize(stream, stream.readInt32(exception), exception);
            chats = Vector.deserialize(stream, Chat::TLdeserialize, exception);
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            participant.serializeToStream(stream);
            Vector.serialize(stream, chats);
            Vector.serialize(stream, users);
        }
    }

    public static class TL_authorization extends TLObject {
        public static final int constructor = 0xad01d61d;

        public int flags;
        public boolean current;
        public boolean official_app;
        public boolean password_pending;
        public boolean encrypted_requests_disabled;
        public boolean call_requests_disabled;
        public boolean unconfirmed;
        public long hash;
        public String device_model;
        public String platform;
        public String system_version;
        public int api_id;
        public String app_name;
        public String app_version;
        public int date_created;
        public int date_active;
        public String ip;
        public String country;
        public String region;

        public static TL_authorization TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_authorization result = TL_authorization.constructor != constructor ? null : new TL_authorization();
            return TLdeserialize(TL_authorization.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            current = hasFlag(flags, FLAG_0);
            official_app = hasFlag(flags, FLAG_1);
            password_pending = hasFlag(flags, FLAG_2);
            encrypted_requests_disabled = hasFlag(flags, FLAG_3);
            call_requests_disabled = hasFlag(flags, FLAG_4);
            unconfirmed = hasFlag(flags, FLAG_5);
            hash = stream.readInt64(exception);
            device_model = stream.readString(exception);
            platform = stream.readString(exception);
            system_version = stream.readString(exception);
            api_id = stream.readInt32(exception);
            app_name = stream.readString(exception);
            app_version = stream.readString(exception);
            date_created = stream.readInt32(exception);
            date_active = stream.readInt32(exception);
            ip = stream.readString(exception);
            country = stream.readString(exception);
            region = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, current);
            flags = setFlag(flags, FLAG_1, official_app);
            flags = setFlag(flags, FLAG_2, password_pending);
            flags = setFlag(flags, FLAG_3, encrypted_requests_disabled);
            flags = setFlag(flags, FLAG_4, call_requests_disabled);
            flags = setFlag(flags, FLAG_5, unconfirmed);
            stream.writeInt32(flags);
            stream.writeInt64(hash);
            stream.writeString(device_model);
            stream.writeString(platform);
            stream.writeString(system_version);
            stream.writeInt32(api_id);
            stream.writeString(app_name);
            stream.writeString(app_version);
            stream.writeInt32(date_created);
            stream.writeInt32(date_active);
            stream.writeString(ip);
            stream.writeString(country);
            stream.writeString(region);
        }
    }

    public static abstract class updates_Difference extends TLObject {
        public ArrayList<Message> new_messages = new ArrayList<>();
        public ArrayList<EncryptedMessage> new_encrypted_messages = new ArrayList<>();
        public ArrayList<Update> other_updates = new ArrayList<>();
        public ArrayList<Chat> chats = new ArrayList<>();
        public ArrayList<User> users = new ArrayList<>();
        public TL_updates_state state;
        public TL_updates_state intermediate_state;
        public int pts;
        public int date;
        public int seq;

        public static updates_Difference TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            updates_Difference result = null;
            switch (constructor) {
                case 0xf49ca0:
                    result = new TL_updates_difference();
                    break;
                case 0xa8fb1981:
                    result = new TL_updates_differenceSlice();
                    break;
                case 0x4afe8f6d:
                    result = new TL_updates_differenceTooLong();
                    break;
                case 0x5d75a138:
                    result = new TL_updates_differenceEmpty();
                    break;
            }
            return TLdeserialize(updates_Difference.class, result, stream, constructor, exception);
        }
    }

    public static class TL_updates_difference extends updates_Difference {
        public static final int constructor = 0xf49ca0;

        public void readParams(InputSerializedData stream, boolean exception) {
            new_messages = Vector.deserialize(stream, Message::TLdeserialize, exception);
            new_encrypted_messages = Vector.deserialize(stream, EncryptedMessage::TLdeserialize, exception);
            other_updates = Vector.deserialize(stream, Update::TLdeserialize, exception);
            chats = Vector.deserialize(stream, Chat::TLdeserialize, exception);
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
            state = TL_updates_state.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            Vector.serialize(stream, new_messages);
            Vector.serialize(stream, new_encrypted_messages);
            Vector.serialize(stream, other_updates);
            Vector.serialize(stream, chats);
            Vector.serialize(stream, users);
            state.serializeToStream(stream);
        }
    }

    public static class TL_updates_differenceSlice extends updates_Difference {
        public static final int constructor = 0xa8fb1981;

        public void readParams(InputSerializedData stream, boolean exception) {
            new_messages = Vector.deserialize(stream, Message::TLdeserialize, exception);
            new_encrypted_messages = Vector.deserialize(stream, EncryptedMessage::TLdeserialize, exception);
            other_updates = Vector.deserialize(stream, Update::TLdeserialize, exception);
            chats = Vector.deserialize(stream, Chat::TLdeserialize, exception);
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
            intermediate_state = TL_updates_state.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            Vector.serialize(stream, new_messages);
            Vector.serialize(stream, new_encrypted_messages);
            Vector.serialize(stream, other_updates);
            Vector.serialize(stream, chats);
            Vector.serialize(stream, users);
            intermediate_state.serializeToStream(stream);
        }
    }

    public static class TL_updates_differenceTooLong extends updates_Difference {
        public static final int constructor = 0x4afe8f6d;

        public void readParams(InputSerializedData stream, boolean exception) {
            pts = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(pts);
        }
    }

    public static class TL_updates_differenceEmpty extends updates_Difference {
        public static final int constructor = 0x5d75a138;

        public void readParams(InputSerializedData stream, boolean exception) {
            date = stream.readInt32(exception);
            seq = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(date);
            stream.writeInt32(seq);
        }
    }

    public static abstract class PrivacyKey extends TLObject {

        public static PrivacyKey TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            PrivacyKey result = null;
            switch (constructor) {
                case TL_privacyKeyStatusTimestamp.constructor:
                    result = new TL_privacyKeyStatusTimestamp();
                    break;
                case TL_privacyKeyPhoneP2P.constructor:
                    result = new TL_privacyKeyPhoneP2P();
                    break;
                case TL_privacyKeyChatInvite.constructor:
                    result = new TL_privacyKeyChatInvite();
                    break;
                case TL_privacyKeyAddedByPhone.constructor:
                    result = new TL_privacyKeyAddedByPhone();
                    break;
                case TL_privacyKeyVoiceMessages.constructor:
                    result = new TL_privacyKeyVoiceMessages();
                    break;
                case TL_privacyKeyAbout.constructor:
                    result = new TL_privacyKeyAbout();
                    break;
                case TL_privacyKeyPhoneCall.constructor:
                    result = new TL_privacyKeyPhoneCall();
                    break;
                case TL_privacyKeyForwards.constructor:
                    result = new TL_privacyKeyForwards();
                    break;
                case TL_privacyKeyPhoneNumber.constructor:
                    result = new TL_privacyKeyPhoneNumber();
                    break;
                case TL_privacyKeyProfilePhoto.constructor:
                    result = new TL_privacyKeyProfilePhoto();
                    break;
                case TL_privacyKeyBirthday.constructor:
                    result = new TL_privacyKeyBirthday();
                    break;
                case TL_privacyKeyStarGiftsAutoSave.constructor:
                    result = new TL_privacyKeyStarGiftsAutoSave();
                    break;
                case TL_privacyKeyNoPaidMessages.constructor:
                    result = new TL_privacyKeyNoPaidMessages();
                    break;
                case TL_privacyKeySavedMusic.constructor:
                    result = new TL_privacyKeySavedMusic();
                    break;
            }
            return TLdeserialize(PrivacyKey.class, result, stream, constructor, exception);
        }
    }

    public static class TL_privacyKeyStatusTimestamp extends PrivacyKey {
        public static final int constructor = 0xbc2eab30;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_privacyKeyPhoneP2P extends PrivacyKey {
        public static final int constructor = 0x39491cc8;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_privacyKeyChatInvite extends PrivacyKey {
        public static final int constructor = 0x500e6dfa;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_privacyKeyPhoneCall extends PrivacyKey {
        public static final int constructor = 0x3d662b7b;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_privacyKeyAddedByPhone extends PrivacyKey {
        public static final int constructor = 0x42ffd42b;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_privacyKeyAbout extends PrivacyKey {
        public static final int constructor = 0xa486b761;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_privacyKeyVoiceMessages extends PrivacyKey {
        public static final int constructor = 0x697f414;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_privacyKeyForwards extends PrivacyKey {
        public static final int constructor = 0x69ec56a3;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_privacyKeyPhoneNumber extends PrivacyKey {
        public static final int constructor = 0xd19ae46d;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_privacyKeyProfilePhoto extends PrivacyKey {
        public static final int constructor = 0x96151fed;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_privacyKeyBirthday extends PrivacyKey {
        public static final int constructor = 0x2000a518;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_privacyKeyStarGiftsAutoSave extends PrivacyKey {
        public static final int constructor = 0x2ca4fdf8;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_privacyKeyNoPaidMessages extends PrivacyKey {
        public static final int constructor = 0x17d348d2;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_privacyKeySavedMusic extends PrivacyKey {
        public static final int constructor = 0xff7a571b;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static abstract class GeoPoint extends TLObject {
        public int flags;
        public double _long;
        public double lat;
        public int accuracy_radius;
        public long access_hash;

        public static GeoPoint TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            GeoPoint result = null;
            switch (constructor) {
                case 0x296f104:
                    result = new TL_geoPoint_layer119();
                    break;
                case 0x2049d70c:
                    result = new TL_geoPoint_layer81();
                    break;
                case 0x1117dd5f:
                    result = new TL_geoPointEmpty();
                    break;
                case 0xb2a2f663:
                    result = new TL_geoPoint();
                    break;
            }
            return TLdeserialize(GeoPoint.class, result, stream, constructor, exception);
        }
    }

    public static class TL_geoPoint_layer119 extends TL_geoPoint {
        public static final int constructor = 0x296f104;

        public void readParams(InputSerializedData stream, boolean exception) {
            _long = stream.readDouble(exception);
            lat = stream.readDouble(exception);
            access_hash = stream.readInt64(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeDouble(_long);
            stream.writeDouble(lat);
            stream.writeInt64(access_hash);
        }
    }

    public static class TL_geoPoint_layer81 extends TL_geoPoint {
        public static final int constructor = 0x2049d70c;

        public void readParams(InputSerializedData stream, boolean exception) {
            _long = stream.readDouble(exception);
            lat = stream.readDouble(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeDouble(_long);
            stream.writeDouble(lat);
        }
    }

    public static class TL_geoPointEmpty extends GeoPoint {
        public static final int constructor = 0x1117dd5f;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_geoPoint extends GeoPoint {
        public static final int constructor = 0xb2a2f663;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            _long = stream.readDouble(exception);
            lat = stream.readDouble(exception);
            access_hash = stream.readInt64(exception);
            if (hasFlag(flags, FLAG_0)) {
                accuracy_radius = stream.readInt32(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            stream.writeDouble(_long);
            stream.writeDouble(lat);
            stream.writeInt64(access_hash);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeInt32(accuracy_radius);
            }
        }
    }

    public static abstract class ChatInvite extends TLObject {

        public int flags;
        public boolean channel;
        public boolean broadcast;
        public boolean isPublic;
        public boolean megagroup;
        public boolean request_needed;
        public String title;
        public String about;
        public Photo photo;
        public int participants_count;
        public ArrayList<User> participants = new ArrayList<>();
        public Chat chat;
        public int expires;
        public boolean verified;
        public boolean scam;
        public boolean fake;
        public boolean can_refulfill_subscription;
        public int color;
        public TL_stars.TL_starsSubscriptionPricing subscription_pricing;
        public long subscription_form_id;
        public TL_bots.botVerification bot_verification;

        public static ChatInvite TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            ChatInvite result = null;
            switch (constructor) {
                case TL_chatInvite.constructor:
                    result = new TL_chatInvite();
                    break;
                case TL_chatInvite_layer195.constructor:
                    result = new TL_chatInvite_layer195();
                    break;
                case TL_chatInvite_layer185.constructor:
                    result = new TL_chatInvite_layer185();
                    break;
                case TL_chatInvite_layer165.constructor:
                    result = new TL_chatInvite_layer165();
                    break;
                case 0x61695cb0:
                    result = new TL_chatInvitePeek();
                    break;
                case 0x5a686d7c:
                    result = new TL_chatInviteAlready();
                    break;
            }
            return TLdeserialize(ChatInvite.class, result, stream, constructor, exception);
        }
    }

    public static class TL_chatInvite extends ChatInvite {
        public static final int constructor = 0x5c9d3702;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            channel = hasFlag(flags, FLAG_0);
            broadcast = hasFlag(flags, FLAG_1);
            isPublic = hasFlag(flags, FLAG_2);
            megagroup = hasFlag(flags, FLAG_3);
            request_needed = hasFlag(flags, FLAG_6);
            verified = hasFlag(flags, FLAG_7);
            scam = hasFlag(flags, FLAG_8);
            fake = hasFlag(flags, FLAG_9);
            can_refulfill_subscription = hasFlag(flags, FLAG_11);
            title = stream.readString(exception);
            boolean hasAbout = hasFlag(flags, FLAG_5);
            if (hasAbout) {
                about = stream.readString(exception);
            }
            photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
            participants_count = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_4)) {
                participants = Vector.deserialize(stream, User::TLdeserialize, exception);
            }
            color = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_10)) {
                subscription_pricing = TL_stars.TL_starsSubscriptionPricing.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_12)) {
                subscription_form_id = stream.readInt64(exception);
            }
            if (hasFlag(flags, FLAG_13)) {
                bot_verification = TL_bots.botVerification.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, channel);
            flags = setFlag(flags, FLAG_1, broadcast);
            flags = setFlag(flags, FLAG_2, isPublic);
            flags = setFlag(flags, FLAG_3, megagroup);
            flags = setFlag(flags, FLAG_5, about != null);
            flags = setFlag(flags, FLAG_6, request_needed);
            flags = setFlag(flags, FLAG_7, verified);
            flags = setFlag(flags, FLAG_8, scam);
            flags = setFlag(flags, FLAG_9, fake);
            flags = setFlag(flags, FLAG_11, can_refulfill_subscription);
            stream.writeInt32(flags);
            stream.writeString(title);
            if (about != null) {
                stream.writeString(about);
            }
            photo.serializeToStream(stream);
            stream.writeInt32(participants_count);
            if (hasFlag(flags, FLAG_4)) {
                Vector.serialize(stream, participants);
            }
            stream.writeInt32(color);
            if (hasFlag(flags, FLAG_10)) {
                subscription_pricing.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_12)) {
                stream.writeInt64(subscription_form_id);
            }
            if (hasFlag(flags, FLAG_13)) {
                bot_verification.serializeToStream(stream);
            }
        }
    }

    public static class TL_chatInvite_layer195 extends TL_chatInvite {
        public static final int constructor = 0xfe65389d;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            channel = hasFlag(flags, FLAG_0);
            broadcast = hasFlag(flags, FLAG_1);
            isPublic = hasFlag(flags, FLAG_2);
            megagroup = hasFlag(flags, FLAG_3);
            request_needed = hasFlag(flags, FLAG_6);
            verified = hasFlag(flags, FLAG_7);
            scam = hasFlag(flags, FLAG_8);
            fake = hasFlag(flags, FLAG_9);
            can_refulfill_subscription = hasFlag(flags, FLAG_11);
            title = stream.readString(exception);
            boolean hasAbout = hasFlag(flags, FLAG_5);
            if (hasAbout) {
                about = stream.readString(exception);
            }
            photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
            participants_count = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_4)) {
                participants = Vector.deserialize(stream, User::TLdeserialize, exception);
            }
            color = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_10)) {
                subscription_pricing = TL_stars.TL_starsSubscriptionPricing.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_12)) {
                subscription_form_id = stream.readInt64(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, channel);
            flags = setFlag(flags, FLAG_1, broadcast);
            flags = setFlag(flags, FLAG_2, isPublic);
            flags = setFlag(flags, FLAG_3, megagroup);
            flags = setFlag(flags, FLAG_5, about != null);
            flags = setFlag(flags, FLAG_6, request_needed);
            flags = setFlag(flags, FLAG_7, verified);
            flags = setFlag(flags, FLAG_8, scam);
            flags = setFlag(flags, FLAG_9, fake);
            flags = setFlag(flags, FLAG_11, can_refulfill_subscription);
            stream.writeInt32(flags);
            stream.writeString(title);
            if (about != null) {
                stream.writeString(about);
            }
            photo.serializeToStream(stream);
            stream.writeInt32(participants_count);
            if (hasFlag(flags, FLAG_4)) {
                Vector.serialize(stream, participants);
            }
            stream.writeInt32(color);
            if (hasFlag(flags, FLAG_10)) {
                subscription_pricing.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_12)) {
                stream.writeInt64(subscription_form_id);
            }
        }
    }

    public static class TL_chatInvite_layer185 extends TL_chatInvite {
        public static final int constructor = 0xcde0ec40;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            channel = hasFlag(flags, FLAG_0);
            broadcast = hasFlag(flags, FLAG_1);
            isPublic = hasFlag(flags, FLAG_2);
            megagroup = hasFlag(flags, FLAG_3);
            request_needed = hasFlag(flags, FLAG_6);
            verified = hasFlag(flags, FLAG_7);
            scam = hasFlag(flags, FLAG_8);
            fake = hasFlag(flags, FLAG_9);
            title = stream.readString(exception);
            boolean hasAbout = hasFlag(flags, FLAG_5);
            if (hasAbout) {
                about = stream.readString(exception);
            }
            photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
            participants_count = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_4)) {
                participants = Vector.deserialize(stream, User::TLdeserialize, exception);
            }
            color = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, channel);
            flags = setFlag(flags, FLAG_1, broadcast);
            flags = setFlag(flags, FLAG_2, isPublic);
            flags = setFlag(flags, FLAG_3, megagroup);
            flags = setFlag(flags, FLAG_5, about != null);
            flags = setFlag(flags, FLAG_6, request_needed);
            flags = setFlag(flags, FLAG_7, verified);
            flags = setFlag(flags, FLAG_8, scam);
            flags = setFlag(flags, FLAG_9, fake);
            stream.writeInt32(flags);
            stream.writeString(title);
            if (about != null) {
                stream.writeString(about);
            }
            photo.serializeToStream(stream);
            stream.writeInt32(participants_count);
            if (hasFlag(flags, FLAG_4)) {
                Vector.serialize(stream, participants);
            }
            stream.writeInt32(color);
        }
    }

    public static class TL_chatInvite_layer165 extends ChatInvite {
        public static final int constructor = 0x300c44c1;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            channel = hasFlag(flags, FLAG_0);
            broadcast = hasFlag(flags, FLAG_1);
            isPublic = hasFlag(flags, FLAG_2);
            megagroup = hasFlag(flags, FLAG_3);
            request_needed = hasFlag(flags, FLAG_6);
            verified = hasFlag(flags, FLAG_7);
            scam = hasFlag(flags, FLAG_8);
            fake = hasFlag(flags, FLAG_9);
            title = stream.readString(exception);
            boolean hasAbout = hasFlag(flags, FLAG_5);
            if (hasAbout) {
                about = stream.readString(exception);
            }
            photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
            participants_count = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_4)) {
                participants = Vector.deserialize(stream, User::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_10)) {
                color = stream.readInt32(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, channel);
            flags = setFlag(flags, FLAG_1, broadcast);
            flags = setFlag(flags, FLAG_2, isPublic);
            flags = setFlag(flags, FLAG_3, megagroup);
            flags = setFlag(flags, FLAG_5, about != null);
            flags = setFlag(flags, FLAG_6, request_needed);
            flags = setFlag(flags, FLAG_7, verified);
            flags = setFlag(flags, FLAG_8, scam);
            flags = setFlag(flags, FLAG_9, fake);
            stream.writeInt32(flags);
            stream.writeString(title);
            if (about != null) {
                stream.writeString(about);
            }
            photo.serializeToStream(stream);
            stream.writeInt32(participants_count);
            if (hasFlag(flags, FLAG_4)) {
                Vector.serialize(stream, participants);
            }
            if (hasFlag(flags, FLAG_10)) {
                stream.writeInt32(color);
            }
        }
    }

    public static class TL_chatInvitePeek extends ChatInvite {
        public static final int constructor = 0x61695cb0;

        public void readParams(InputSerializedData stream, boolean exception) {
            chat = Chat.TLdeserialize(stream, stream.readInt32(exception), exception);
            expires = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            chat.serializeToStream(stream);
            stream.writeInt32(expires);
        }
    }

    public static class TL_chatInviteAlready extends ChatInvite {
        public static final int constructor = 0x5a686d7c;

        public void readParams(InputSerializedData stream, boolean exception) {
            chat = Chat.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            chat.serializeToStream(stream);
        }
    }

    public static class InputGroupCall extends TLObject {

        public long id;
        public long access_hash;
        public String slug;
        public int msg_id;

        public static InputGroupCall TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            InputGroupCall result = null;
            switch (constructor) {
                case TL_inputGroupCall.constructor:
                    result = new TL_inputGroupCall();
                    break;
                case TL_inputGroupCallSlug.constructor:
                    result = new TL_inputGroupCallSlug();
                    break;
                case TL_inputGroupCallInviteMessage.constructor:
                    result = new TL_inputGroupCallInviteMessage();
                    break;
            }
            return TLdeserialize(InputGroupCall.class, result, stream, constructor, exception);
        }
    }

    public static class TL_inputGroupCall extends InputGroupCall {
        public static final int constructor = 0xd8aa840f;

        public void readParams(InputSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
            access_hash = stream.readInt64(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(id);
            stream.writeInt64(access_hash);
        }
    }

    public static class TL_inputGroupCallSlug extends InputGroupCall {
        public static final int constructor = 0xfe06823f;

        public void readParams(InputSerializedData stream, boolean exception) {
            slug = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(slug);
        }
    }

    public static class TL_inputGroupCallInviteMessage extends InputGroupCall {
        public static final int constructor = 0x8c10603f;

        public void readParams(InputSerializedData stream, boolean exception) {
            msg_id = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(msg_id);
        }
    }

    public static abstract class help_AppUpdate extends TLObject {

        public static help_AppUpdate TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            help_AppUpdate result = null;
            switch (constructor) {
                case 0xccbbce30:
                    result = new TL_help_appUpdate();
                    break;
                case 0xc45a6536:
                    result = new TL_help_noAppUpdate();
                    break;
            }
            return TLdeserialize(help_AppUpdate.class, result, stream, constructor, exception);
        }
    }

    @Keep
    public static class TL_help_appUpdate extends help_AppUpdate {
        public static final int constructor = 0xccbbce30;

        public int flags;
        public boolean can_not_skip;
        public int id;
        public String version;
        public String text;
        public ArrayList<MessageEntity> entities = new ArrayList<>();
        public Document document;
        public String url;
        public Document sticker;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            can_not_skip = hasFlag(flags, FLAG_0);
            id = stream.readInt32(exception);
            version = stream.readString(exception);
            text = stream.readString(exception);
            entities = Vector.deserialize(stream, MessageEntity::TLdeserialize, exception);
            if (hasFlag(flags, FLAG_1)) {
                document = Document.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                url = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_3)) {
                sticker = Document.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, can_not_skip);
            stream.writeInt32(flags);
            stream.writeInt32(id);
            stream.writeString(version);
            stream.writeString(text);
            Vector.serialize(stream, entities);
            if (hasFlag(flags, FLAG_1)) {
                document.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeString(url);
            }
            if (hasFlag(flags, FLAG_3)) {
                sticker.serializeToStream(stream);
            }
        }
    }

    public static class TL_help_noAppUpdate extends help_AppUpdate {
        public static final int constructor = 0xc45a6536;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_messages_affectedFoundMessages extends TLObject {
        public static final int constructor = 0xef8d3e6c;

        public int pts;
        public int pts_count;
        public int offset;
        public ArrayList<Integer> messages = new ArrayList<>();

        public static TL_messages_affectedFoundMessages TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_messages_affectedFoundMessages result = TL_messages_affectedFoundMessages.constructor != constructor ? null : new TL_messages_affectedFoundMessages();
            return TLdeserialize(TL_messages_affectedFoundMessages.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            pts = stream.readInt32(exception);
            pts_count = stream.readInt32(exception);
            offset = stream.readInt32(exception);
            messages = Vector.deserializeInt(stream, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(pts);
            stream.writeInt32(pts_count);
            stream.writeInt32(offset);
            Vector.serializeInt(stream, messages);
        }
    }

    public static class TL_channelAdminLogEvent extends TLObject {
        public static final int constructor = 0x1fad68cd;

        public long id;
        public int date;
        public long user_id;
        public ChannelAdminLogEventAction action;

        public static TL_channelAdminLogEvent TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_channelAdminLogEvent result = TL_channelAdminLogEvent.constructor != constructor ? null : new TL_channelAdminLogEvent();
            return TLdeserialize(TL_channelAdminLogEvent.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
            date = stream.readInt32(exception);
            user_id = stream.readInt64(exception);
            action = ChannelAdminLogEventAction.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(id);
            stream.writeInt32(date);
            stream.writeInt64(user_id);
            action.serializeToStream(stream);
        }
    }

    public static abstract class messages_FavedStickers extends TLObject {
        public long hash;
        public ArrayList<TL_stickerPack> packs = new ArrayList<>();
        public ArrayList<Document> stickers = new ArrayList<>();

        public static messages_FavedStickers TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            messages_FavedStickers result = null;
            switch (constructor) {
                case 0x9e8fa6d3:
                    result = new TL_messages_favedStickersNotModified();
                    break;
                case 0x2cb51097:
                    result = new TL_messages_favedStickers();
                    break;
            }
            return TLdeserialize(messages_FavedStickers.class, result, stream, constructor, exception);
        }
    }

    public static class TL_messages_favedStickersNotModified extends messages_FavedStickers {
        public static final int constructor = 0x9e8fa6d3;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_messages_favedStickers extends messages_FavedStickers {
        public static final int constructor = 0x2cb51097;

        public void readParams(InputSerializedData stream, boolean exception) {
            hash = stream.readInt64(exception);
            packs = Vector.deserialize(stream, TL_stickerPack::TLdeserialize, exception);
            stickers = Vector.deserialize(stream, Document::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(hash);
            Vector.serialize(stream, packs);
            Vector.serialize(stream, stickers);
        }
    }

    public static class TL_langPackLanguage extends TLObject {
        public static final int constructor = 0xeeca5ce3;

        public int flags;
        public boolean official;
        public boolean rtl;
        public String name;
        public String native_name;
        public String lang_code;
        public String base_lang_code;
        public String plural_code;
        public int strings_count;
        public int translated_count;
        public String translations_url;

        public static TL_langPackLanguage TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_langPackLanguage result = TL_langPackLanguage.constructor != constructor ? null : new TL_langPackLanguage();
            return TLdeserialize(TL_langPackLanguage.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            official = hasFlag(flags, FLAG_0);
            rtl = hasFlag(flags, FLAG_2);
            name = stream.readString(exception);
            native_name = stream.readString(exception);
            lang_code = stream.readString(exception);
            if (hasFlag(flags, FLAG_1)) {
                base_lang_code = stream.readString(exception);
            }
            plural_code = stream.readString(exception);
            strings_count = stream.readInt32(exception);
            translated_count = stream.readInt32(exception);
            translations_url = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, official);
            flags = setFlag(flags, FLAG_2, rtl);
            stream.writeInt32(flags);
            stream.writeString(name);
            stream.writeString(native_name);
            stream.writeString(lang_code);
            if (hasFlag(flags, FLAG_1)) {
                stream.writeString(base_lang_code);
            }
            stream.writeString(plural_code);
            stream.writeInt32(strings_count);
            stream.writeInt32(translated_count);
            stream.writeString(translations_url);
        }
    }

    public static class TL_chatInviteImporter extends TLObject {
        public static final int constructor = 0x8c5adfd9;

        public int flags;
        public boolean requested;
        public long user_id;
        public int date;
        public String about;
        public long approved_by;
        public boolean via_chatlist;

        public static TL_chatInviteImporter TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_chatInviteImporter result = TL_chatInviteImporter.constructor != constructor ? null : new TL_chatInviteImporter();
            return TLdeserialize(TL_chatInviteImporter.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            requested = hasFlag(flags, FLAG_0);
            via_chatlist = hasFlag(flags, FLAG_3);
            user_id = stream.readInt64(exception);
            date = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_2)) {
                about = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_1)) {
                approved_by = stream.readInt64(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, requested);
            flags = setFlag(flags, FLAG_3, via_chatlist);
            stream.writeInt32(flags);
            stream.writeInt64(user_id);
            stream.writeInt32(date);
            if (hasFlag(flags, FLAG_2)) {
                stream.writeString(about);
            }
            if (hasFlag(flags, FLAG_1)) {
                stream.writeInt64(approved_by);
            }
        }
    }

    public static abstract class SendMessageAction extends TLObject {
        public int progress;

        public static SendMessageAction TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            return TLdeserialize(SendMessageAction.class, fromConstructor(constructor), stream, constructor, exception);
        }

        private static SendMessageAction fromConstructor(int constructor) {
            switch (constructor) {
                case TL_sendMessageTextDraftAction.constructor:
                    return new TL_sendMessageTextDraftAction();
                case TL_sendMessageRichMessageDraftAction.constructor:
                    return new TL_sendMessageRichMessageDraftAction();
                case TL_sendMessageStopDraftAction.constructor:
                    return new TL_sendMessageStopDraftAction();
                case TL_sendMessageGamePlayAction.constructor:
                    return new TL_sendMessageGamePlayAction();
                case TL_sendMessageRecordAudioAction.constructor:
                    return new TL_sendMessageRecordAudioAction();
                case TL_sendMessageUploadVideoAction_old.constructor:
                    return new TL_sendMessageUploadVideoAction_old();
                case TL_sendMessageUploadAudioAction_old.constructor:
                    return new TL_sendMessageUploadAudioAction_old();
                case TL_sendMessageUploadAudioAction.constructor:
                    return new TL_sendMessageUploadAudioAction();
                case TL_sendMessageUploadPhotoAction.constructor:
                    return new TL_sendMessageUploadPhotoAction();
                case TL_sendMessageUploadDocumentAction_old.constructor:
                    return new TL_sendMessageUploadDocumentAction_old();
                case TL_sendMessageUploadVideoAction.constructor:
                    return new TL_sendMessageUploadVideoAction();
                case TL_sendMessageCancelAction.constructor:
                    return new TL_sendMessageCancelAction();
                case TL_sendMessageGeoLocationAction.constructor:
                    return new TL_sendMessageGeoLocationAction();
                case TL_sendMessageChooseContactAction.constructor:
                    return new TL_sendMessageChooseContactAction();
                case TL_sendMessageChooseStickerAction.constructor:
                    return new TL_sendMessageChooseStickerAction();
                case TL_sendMessageRecordRoundAction.constructor:
                    return new TL_sendMessageRecordRoundAction();
                case TL_sendMessageUploadRoundAction.constructor:
                    return new TL_sendMessageUploadRoundAction();
                case TL_sendMessageUploadRoundAction_layer66.constructor:
                    return new TL_sendMessageUploadRoundAction_layer66();
                case TL_sendMessageTypingAction.constructor:
                    return new TL_sendMessageTypingAction();
                case TL_sendMessageHistoryImportAction.constructor:
                    return new TL_sendMessageHistoryImportAction();
                case TL_sendMessageUploadPhotoAction_old.constructor:
                    return new TL_sendMessageUploadPhotoAction_old();
                case TL_sendMessageUploadDocumentAction.constructor:
                    return new TL_sendMessageUploadDocumentAction();
                case TL_speakingInGroupCallAction.constructor:
                    return new TL_speakingInGroupCallAction();
                case TL_sendMessageRecordVideoAction.constructor:
                    return new TL_sendMessageRecordVideoAction();
                case TL_sendMessageEmojiInteraction.constructor:
                    return new TL_sendMessageEmojiInteraction();
                case TL_sendMessageEmojiInteractionSeen.constructor:
                    return new TL_sendMessageEmojiInteractionSeen();
                default:
                    return null;
            }
        }
    }

    public static class TL_sendMessageTextDraftAction extends SendMessageAction {
        public static final int constructor = 0x3630b85a;

        public boolean can_stop;
        public boolean keep_on_stop;
        public long random_id;
        public TL_textWithEntities text;

        public int flags;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            can_stop = hasFlag(flags, FLAG_0);
            keep_on_stop = hasFlag(flags, FLAG_1);
            random_id = stream.readInt64(exception);
            text = TL_textWithEntities.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, can_stop);
            flags = setFlag(flags, FLAG_1, keep_on_stop);
            stream.writeInt32(flags);
            stream.writeInt64(random_id);
            text.serializeToStream(stream);
        }
    }

    public static class TL_sendMessageRichMessageDraftAction extends SendMessageAction {
        public static final int constructor = 0x52564893;

        public boolean can_stop;
        public boolean keep_on_stop;
        public long random_id;
        public TL_iv.RichMessage rich_message;

        public int flags;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            can_stop = hasFlag(flags, FLAG_0);
            keep_on_stop = hasFlag(flags, FLAG_1);
            random_id = stream.readInt64(exception);
            rich_message = TL_iv.RichMessage.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, can_stop);
            flags = setFlag(flags, FLAG_1, keep_on_stop);
            stream.writeInt32(flags);
            stream.writeInt64(random_id);
            rich_message.serializeToStream(stream);
        }
    }

    public static class TL_sendMessageStopDraftAction extends SendMessageAction {
        public static final int constructor = 0xfbf902b0;

        public long random_id;

        public void readParams(InputSerializedData stream, boolean exception) {
            random_id = stream.readInt64(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(random_id);
        }
    }

    public static class TL_sendMessageGamePlayAction extends SendMessageAction {
        public static final int constructor = 0xdd6a8f48;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_sendMessageRecordAudioAction extends SendMessageAction {
        public static final int constructor = 0xd52f73f7;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_sendMessageUploadVideoAction_old extends TL_sendMessageUploadVideoAction {
        public static final int constructor = 0x92042ff7;

        public void readParams(InputSerializedData stream, boolean exception) {
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_sendMessageEmojiInteraction extends SendMessageAction {
        public static final int constructor = 0x25972bcb;

        public String emoticon;
        public int msg_id;
        public TL_dataJSON interaction;

        public void readParams(InputSerializedData stream, boolean exception) {
            emoticon = stream.readString(exception);
            msg_id = stream.readInt32(exception);
            interaction = TL_dataJSON.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(emoticon);
            stream.writeInt32(msg_id);
            interaction.serializeToStream(stream);
        }
    }

    public static class TL_sendMessageUploadAudioAction_old extends TL_sendMessageUploadAudioAction {
        public static final int constructor = 0xe6ac8a6f;

        public void readParams(InputSerializedData stream, boolean exception) {
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_sendMessageUploadAudioAction extends SendMessageAction {
        public static final int constructor = 0xf351d7ab;

        public void readParams(InputSerializedData stream, boolean exception) {
            progress = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(progress);
        }
    }

    public static class TL_sendMessageUploadPhotoAction extends SendMessageAction {
        public static final int constructor = 0xd1d34a26;

        public void readParams(InputSerializedData stream, boolean exception) {
            progress = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(progress);
        }
    }

    public static class TL_sendMessageUploadDocumentAction_old extends TL_sendMessageUploadDocumentAction {
        public static final int constructor = 0x8faee98e;

        public void readParams(InputSerializedData stream, boolean exception) {
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_sendMessageUploadVideoAction extends SendMessageAction {
        public static final int constructor = 0xe9763aec;

        public void readParams(InputSerializedData stream, boolean exception) {
            progress = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(progress);
        }
    }

    public static class TL_sendMessageCancelAction extends SendMessageAction {
        public static final int constructor = 0xfd5ec8f5;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_sendMessageGeoLocationAction extends SendMessageAction {
        public static final int constructor = 0x176f8ba1;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_sendMessageChooseContactAction extends SendMessageAction {
        public static final int constructor = 0x628cbc6f;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_sendMessageChooseStickerAction extends SendMessageAction {
        public static final int constructor = 0xb05ac6b1;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_sendMessageRecordRoundAction extends SendMessageAction {
        public static final int constructor = 0x88f27fbc;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_sendMessageUploadRoundAction extends SendMessageAction {
        public static final int constructor = 0x243e1c66;

        public void readParams(InputSerializedData stream, boolean exception) {
            progress = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(progress);
        }
    }

    public static class TL_sendMessageUploadRoundAction_layer66 extends TL_sendMessageUploadRoundAction {
        public static final int constructor = 0xbb718624;

        public void readParams(InputSerializedData stream, boolean exception) {
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_sendMessageEmojiInteractionSeen extends SendMessageAction {
        public static final int constructor = 0xb665902e;

        public String emoticon;

        public void readParams(InputSerializedData stream, boolean exception) {
            emoticon = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(emoticon);
        }
    }

    public static class TL_sendMessageTypingAction extends SendMessageAction {
        public static final int constructor = 0x16bf744e;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_sendMessageHistoryImportAction extends SendMessageAction {
        public static final int constructor = 0xdbda9246;

        public int progress;

        public void readParams(InputSerializedData stream, boolean exception) {
            progress = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(progress);
        }
    }

    public static class TL_sendMessageUploadPhotoAction_old extends TL_sendMessageUploadPhotoAction {
        public static final int constructor = 0x990a3c1a;

        public void readParams(InputSerializedData stream, boolean exception) {
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_sendMessageUploadDocumentAction extends SendMessageAction {
        public static final int constructor = 0xaa0cd9e4;

        public void readParams(InputSerializedData stream, boolean exception) {
            progress = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(progress);
        }
    }

    public static class TL_speakingInGroupCallAction extends SendMessageAction {
        public static final int constructor = 0xd92c2285;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_sendMessageRecordVideoAction extends SendMessageAction {
        public static final int constructor = 0xa187d66f;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static abstract class auth_SentCodeType extends TLObject {
        public int flags;
        public String url;
        public int length;
        public String pattern;
        public String prefix;
        public boolean apple_signin_allowed;
        public boolean google_signin_allowed;
        public String email_pattern;
        public int next_phone_login_date;
        public byte[] nonce;
        public long play_integrity_project_id;
        public byte[] play_integrity_nonce;
        public String receipt;
        public int push_timeout;
        public int reset_available_period;
        public int reset_pending_date;
        public String beginning;
        public boolean verifiedFirebase; //custom

        public static auth_SentCodeType TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            auth_SentCodeType result = null;
            switch (constructor) {
                case 0x3dbb5986:
                    result = new TL_auth_sentCodeTypeApp();
                    break;
                case 0x5353e5a7:
                    result = new TL_auth_sentCodeTypeCall();
                    break;
                case 0xf450f59b:
                    result = new TL_auth_sentCodeTypeEmailCode();
                    break;
                case 0xa5491dea:
                    result = new TL_auth_sentCodeTypeSetUpEmailRequired();
                    break;
                case 0xab03c6d9:
                    result = new TL_auth_sentCodeTypeFlashCall();
                    break;
                case 0x82006484:
                    result = new TL_auth_sentCodeTypeMissedCall();
                    break;
                case 0xc000bba2:
                    result = new TL_auth_sentCodeTypeSms();
                    break;
                case 0xd9565c39:
                    result = new TL_auth_sentCodeTypeFragmentSms();
                    break;
                case 0x9fd736:
                    result = new TL_auth_sentCodeTypeFirebaseSms();
                    break;
                case 0xa416ac81:
                    result = new TL_auth_sentCodeTypeSmsWord();
                    break;
                case 0xb37794af:
                    result = new TL_auth_sentCodeTypeSmsPhrase();
                    break;
            }
            return TLdeserialize(auth_SentCodeType.class, result, stream, constructor, exception);
        }
    }

    public static class TL_auth_sentCodeTypeApp extends auth_SentCodeType {
        public static final int constructor = 0x3dbb5986;

        public void readParams(InputSerializedData stream, boolean exception) {
            length = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(length);
        }
    }

    public static class TL_auth_sentCodeTypeCall extends auth_SentCodeType {
        public static final int constructor = 0x5353e5a7;

        public void readParams(InputSerializedData stream, boolean exception) {
            length = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(length);
        }
    }

    public static class TL_auth_sentCodeTypeEmailCode extends auth_SentCodeType {
        public static final int constructor = 0xf450f59b;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            apple_signin_allowed = hasFlag(flags, FLAG_0);
            google_signin_allowed = hasFlag(flags, FLAG_1);
            email_pattern = stream.readString(exception);
            length = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_3)) {
                reset_available_period = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                reset_pending_date = stream.readInt32(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, apple_signin_allowed);
            flags = setFlag(flags, FLAG_1, google_signin_allowed);
            stream.writeInt32(flags);
            stream.writeString(email_pattern);
            stream.writeInt32(length);
            if (hasFlag(flags, FLAG_3)) {
                stream.writeInt32(reset_available_period);
            }
            if (hasFlag(flags, FLAG_4)) {
                stream.writeInt32(reset_pending_date);
            }
        }
    }

    public static class TL_auth_sentCodeTypeSetUpEmailRequired extends auth_SentCodeType {
        public static final int constructor = 0xa5491dea;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            apple_signin_allowed = hasFlag(flags, FLAG_0);
            google_signin_allowed = hasFlag(flags, FLAG_1);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, apple_signin_allowed);
            flags = setFlag(flags, FLAG_1, google_signin_allowed);
            stream.writeInt32(flags);
        }
    }

    public static class TL_auth_sentCodeTypeFlashCall extends auth_SentCodeType {
        public static final int constructor = 0xab03c6d9;

        public void readParams(InputSerializedData stream, boolean exception) {
            pattern = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(pattern);
        }
    }

    public static class TL_auth_sentCodeTypeMissedCall extends auth_SentCodeType {
        public static final int constructor = 0x82006484;

        public void readParams(InputSerializedData stream, boolean exception) {
            prefix = stream.readString(exception);
            length = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(prefix);
            stream.writeInt32(length);
        }
    }

    public static class TL_auth_sentCodeTypeSms extends auth_SentCodeType {
        public static final int constructor = 0xc000bba2;

        public void readParams(InputSerializedData stream, boolean exception) {
            length = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(length);
        }
    }

    public static class TL_auth_sentCodeTypeFragmentSms extends auth_SentCodeType {
        public static final int constructor = 0xd9565c39;

        public void readParams(InputSerializedData stream, boolean exception) {
            url = stream.readString(exception);
            length = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(url);
            stream.writeInt32(length);
        }
    }

    public static class TL_auth_sentCodeTypeFirebaseSms extends auth_SentCodeType {
        public static final int constructor = 0x9fd736;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_0)) {
                nonce = stream.readByteArray(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                play_integrity_project_id = stream.readInt64(exception);
                play_integrity_nonce = stream.readByteArray(exception);
            }
            if (hasFlag(flags, FLAG_1)) {
                receipt = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_1)) {
                push_timeout = stream.readInt32(exception);
            }
            length = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeByteArray(nonce);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt64(play_integrity_project_id);
                stream.writeByteArray(play_integrity_nonce);
            }
            if (hasFlag(flags, FLAG_1)) {
                stream.writeString(receipt);
            }
            if (hasFlag(flags, FLAG_1)) {
                stream.writeInt32(push_timeout);
            }
            stream.writeInt32(length);
        }
    }

    public static class TL_auth_sentCodeTypeSmsWord extends auth_SentCodeType {
        public static final int constructor = 0xa416ac81;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_0)) {
                beginning = stream.readString(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeString(beginning);
            }
        }
    }

    public static class TL_auth_sentCodeTypeSmsPhrase extends auth_SentCodeType {
        public static final int constructor = 0xb37794af;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_0)) {
                beginning = stream.readString(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeString(beginning);
            }
        }
    }

    public static abstract class messages_StickerSetInstallResult extends TLObject {
        public ArrayList<StickerSetCovered> sets = new ArrayList<>();

        public static messages_StickerSetInstallResult TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            messages_StickerSetInstallResult result = null;
            switch (constructor) {
                case 0x38641628:
                    result = new TL_messages_stickerSetInstallResultSuccess();
                    break;
                case 0x35e410a8:
                    result = new TL_messages_stickerSetInstallResultArchive();
                    break;
            }
            return TLdeserialize(messages_StickerSetInstallResult.class, result, stream, constructor, exception);
        }
    }

    public static class TL_messages_stickerSetInstallResultSuccess extends messages_StickerSetInstallResult {
        public static final int constructor = 0x38641628;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_messages_stickerSetInstallResultArchive extends messages_StickerSetInstallResult {
        public static final int constructor = 0x35e410a8;

        public void readParams(InputSerializedData stream, boolean exception) {
            sets = Vector.deserialize(stream, StickerSetCovered::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            Vector.serialize(stream, sets);
        }
    }

    public static class PeerSettings extends TLObject {

        public int flags;
        public boolean report_spam;
        public boolean add_contact;
        public boolean block_contact;
        public boolean share_contact;
        public boolean need_contacts_exception;
        public boolean report_geo;
        public boolean autoarchived;
        public boolean invite_members;
        public boolean request_chat_broadcast;
        public int geo_distance;
        public String request_chat_title;
        public int request_chat_date;
        public boolean business_bot_paused;
        public boolean business_bot_can_reply;
        public long business_bot_id;
        public String business_bot_manage_url;
        public long charge_paid_message_stars;
        public String registration_month;
        public String phone_country;
        public int name_change_date;
        public int photo_change_date;

        public static PeerSettings TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            PeerSettings result = null;
            switch (constructor) {
                case TL_peerSettings.constructor:
                    result = new TL_peerSettings();
                    break;
                case TL_peerSettings_layer199.constructor:
                    result = new TL_peerSettings_layer199();
                    break;
                case TL_peerSettings_layer176.constructor:
                    result = new TL_peerSettings_layer176();
                    break;
            }
            return TLdeserialize(PeerSettings.class, result, stream, constructor, exception);
        }
    }

    public static class TL_peerSettings extends PeerSettings {
        public static final int constructor = 0xf47741f7;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            report_spam = hasFlag(flags, FLAG_0);
            add_contact = hasFlag(flags, FLAG_1);
            block_contact = hasFlag(flags, FLAG_2);
            share_contact = hasFlag(flags, FLAG_3);
            need_contacts_exception = hasFlag(flags, FLAG_4);
            report_geo = hasFlag(flags, FLAG_5);
            autoarchived = hasFlag(flags, FLAG_7);
            invite_members = hasFlag(flags, FLAG_8);
            request_chat_broadcast = hasFlag(flags, FLAG_10);
            business_bot_paused = hasFlag(flags, FLAG_11);
            business_bot_can_reply = hasFlag(flags, FLAG_12);
            if (hasFlag(flags, FLAG_6)) {
                geo_distance = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_9)) {
                request_chat_title = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_9)) {
                request_chat_date = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_13)) {
                business_bot_id = stream.readInt64(exception);
            }
            if (hasFlag(flags, FLAG_13)) {
                business_bot_manage_url = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_14)) {
                charge_paid_message_stars = stream.readInt64(exception);
            }
            if (hasFlag(flags, FLAG_15)) {
                registration_month = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_16)) {
                phone_country = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_17)) {
                name_change_date = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_18)) {
                photo_change_date = stream.readInt32(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, report_spam);
            flags = setFlag(flags, FLAG_1, add_contact);
            flags = setFlag(flags, FLAG_2, block_contact);
            flags = setFlag(flags, FLAG_3, share_contact);
            flags = setFlag(flags, FLAG_4, need_contacts_exception);
            flags = setFlag(flags, FLAG_5, report_geo);
            flags = setFlag(flags, FLAG_7, autoarchived);
            flags = setFlag(flags, FLAG_8, invite_members);
            flags = setFlag(flags, FLAG_10, request_chat_broadcast);
            flags = setFlag(flags, FLAG_11, business_bot_paused);
            flags = setFlag(flags, FLAG_12, business_bot_can_reply);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_6)) {
                stream.writeInt32(geo_distance);
            }
            if (hasFlag(flags, FLAG_9)) {
                stream.writeString(request_chat_title);
            }
            if (hasFlag(flags, FLAG_9)) {
                stream.writeInt32(request_chat_date);
            }
            if (hasFlag(flags, FLAG_13)) {
                stream.writeInt64(business_bot_id);
            }
            if (hasFlag(flags, FLAG_13)) {
                stream.writeString(business_bot_manage_url);
            }
            if (hasFlag(flags, FLAG_14)) {
                stream.writeInt64(charge_paid_message_stars);
            }
            if (hasFlag(flags, FLAG_15)) {
                stream.writeString(registration_month);
            }
            if (hasFlag(flags, FLAG_16)) {
                stream.writeString(phone_country);
            }
            if (hasFlag(flags, FLAG_17)) {
                stream.writeInt32(name_change_date);
            }
            if (hasFlag(flags, FLAG_18)) {
                stream.writeInt32(photo_change_date);
            }
        }
    }

    public static class TL_peerSettings_layer199 extends TL_peerSettings {
        public static final int constructor = 0xacd66c5e;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            report_spam = hasFlag(flags, FLAG_0);
            add_contact = hasFlag(flags, FLAG_1);
            block_contact = hasFlag(flags, FLAG_2);
            share_contact = hasFlag(flags, FLAG_3);
            need_contacts_exception = hasFlag(flags, FLAG_4);
            report_geo = hasFlag(flags, FLAG_5);
            autoarchived = hasFlag(flags, FLAG_7);
            invite_members = hasFlag(flags, FLAG_8);
            request_chat_broadcast = hasFlag(flags, FLAG_10);
            business_bot_paused = hasFlag(flags, FLAG_11);
            business_bot_can_reply = hasFlag(flags, FLAG_12);
            if (hasFlag(flags, FLAG_6)) {
                geo_distance = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_9)) {
                request_chat_title = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_9)) {
                request_chat_date = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_13)) {
                business_bot_id = stream.readInt64(exception);
            }
            if (hasFlag(flags, FLAG_13)) {
                business_bot_manage_url = stream.readString(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, report_spam);
            flags = setFlag(flags, FLAG_1, add_contact);
            flags = setFlag(flags, FLAG_2, block_contact);
            flags = setFlag(flags, FLAG_3, share_contact);
            flags = setFlag(flags, FLAG_4, need_contacts_exception);
            flags = setFlag(flags, FLAG_5, report_geo);
            flags = setFlag(flags, FLAG_7, autoarchived);
            flags = setFlag(flags, FLAG_8, invite_members);
            flags = setFlag(flags, FLAG_10, request_chat_broadcast);
            flags = setFlag(flags, FLAG_11, business_bot_paused);
            flags = setFlag(flags, FLAG_12, business_bot_can_reply);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_6)) {
                stream.writeInt32(geo_distance);
            }
            if (hasFlag(flags, FLAG_9)) {
                stream.writeString(request_chat_title);
            }
            if (hasFlag(flags, FLAG_9)) {
                stream.writeInt32(request_chat_date);
            }
            if (hasFlag(flags, FLAG_13)) {
                stream.writeInt64(business_bot_id);
            }
            if (hasFlag(flags, FLAG_13)) {
                stream.writeString(business_bot_manage_url);
            }
        }
    }

    public static class TL_peerSettings_layer176 extends TL_peerSettings {
        public static final int constructor = 0xa518110d;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            report_spam = hasFlag(flags, FLAG_0);
            add_contact = hasFlag(flags, FLAG_1);
            block_contact = hasFlag(flags, FLAG_2);
            share_contact = hasFlag(flags, FLAG_3);
            need_contacts_exception = hasFlag(flags, FLAG_4);
            report_geo = hasFlag(flags, FLAG_5);
            autoarchived = hasFlag(flags, FLAG_7);
            invite_members = hasFlag(flags, FLAG_8);
            request_chat_broadcast = hasFlag(flags, FLAG_10);
            if (hasFlag(flags, FLAG_6)) {
                geo_distance = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_9)) {
                request_chat_title = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_9)) {
                request_chat_date = stream.readInt32(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, report_spam);
            flags = setFlag(flags, FLAG_1, add_contact);
            flags = setFlag(flags, FLAG_2, block_contact);
            flags = setFlag(flags, FLAG_3, share_contact);
            flags = setFlag(flags, FLAG_4, need_contacts_exception);
            flags = setFlag(flags, FLAG_5, report_geo);
            flags = setFlag(flags, FLAG_7, autoarchived);
            flags = setFlag(flags, FLAG_8, invite_members);
            flags = setFlag(flags, FLAG_10, request_chat_broadcast);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_6)) {
                stream.writeInt32(geo_distance);
            }
            if (hasFlag(flags, FLAG_9)) {
                stream.writeString(request_chat_title);
            }
            if (hasFlag(flags, FLAG_9)) {
                stream.writeInt32(request_chat_date);
            }
        }
    }

    public static class TL_readParticipantDate extends TLObject {
        public static final int constructor = 0x4a4ff172;

        public long user_id;
        public int date;

        public static TL_readParticipantDate TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_readParticipantDate result = TL_readParticipantDate.constructor != constructor ? null : new TL_readParticipantDate();
            return TLdeserialize(TL_readParticipantDate.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            user_id = stream.readInt64(exception);
            date = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(user_id);
            stream.writeInt32(date);
        }
    }

    public static abstract class InputDialogPeer extends TLObject {

        public static InputDialogPeer TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            return TLdeserialize(InputDialogPeer.class, fromConstructor(constructor), stream, constructor, exception);
        }

        private static InputDialogPeer fromConstructor(int constructor) {
            switch (constructor) {
                case TL_inputDialogPeer.constructor:
                    return new TL_inputDialogPeer();
                case TL_inputDialogPeerFolder.constructor:
                    return new TL_inputDialogPeerFolder();
                case TL_inputDialogPeerCommunity.constructor:
                    return new TL_inputDialogPeerCommunity();
                default:
                    return null;
            }
        }
    }

    public static class TL_inputDialogPeer extends InputDialogPeer {
        public static final int constructor = 0xfcaafeb7;

        public InputPeer peer;

        public void readParams(InputSerializedData stream, boolean exception) {
            peer = InputPeer.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            peer.serializeToStream(stream);
        }
    }

    public static class TL_inputDialogPeerFolder extends InputDialogPeer {
        public static final int constructor = 0x64600527;

        public int folder_id;

        public void readParams(InputSerializedData stream, boolean exception) {
            folder_id = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(folder_id);
        }
    }

    public static class TL_inputDialogPeerCommunity extends InputDialogPeer {
        public static final int constructor = 0x69EF72C4;

        public InputChannel community;

        public void readParams(InputSerializedData stream, boolean exception) {
            community = InputChannel.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            community.serializeToStream(stream);
        }
    }

    public static abstract class payments_PaymentResult extends TLObject {

        public static payments_PaymentResult TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            payments_PaymentResult result = null;
            switch (constructor) {
                case 0xd8411139:
                    result = new TL_payments_paymentVerificationNeeded();
                    break;
                case 0x4e5f810d:
                    result = new TL_payments_paymentResult();
                    break;
            }
            return TLdeserialize(payments_PaymentResult.class, result, stream, constructor, exception);
        }
    }

    public static class TL_payments_paymentVerificationNeeded extends payments_PaymentResult {
        public static final int constructor = 0xd8411139;

        public String url;

        public void readParams(InputSerializedData stream, boolean exception) {
            url = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(url);
        }
    }

    public static class TL_payments_paymentResult extends payments_PaymentResult {
        public static final int constructor = 0x4e5f810d;

        public Updates updates;

        public void readParams(InputSerializedData stream, boolean exception) {
            updates = Updates.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            updates.serializeToStream(stream);
        }
    }

    public static class TL_channels_adminLogResults extends TLObject {
        public static final int constructor = 0xed8af74d;

        public ArrayList<TL_channelAdminLogEvent> events = new ArrayList<>();
        public ArrayList<Chat> chats = new ArrayList<>();
        public ArrayList<User> users = new ArrayList<>();

        public static TL_channels_adminLogResults TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_channels_adminLogResults result = TL_channels_adminLogResults.constructor != constructor ? null : new TL_channels_adminLogResults();
            return TLdeserialize(TL_channels_adminLogResults.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            events = Vector.deserialize(stream, TL_channelAdminLogEvent::TLdeserialize, exception);
            chats = Vector.deserialize(stream, Chat::TLdeserialize, exception);
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            Vector.serialize(stream, events);
            Vector.serialize(stream, chats);
            Vector.serialize(stream, users);
        }
    }

    public static class TL_inputPhoneContact extends TLObject {
        public static final int constructor = 0x6a1dc4be;

        public int flags;
        public long client_id;
        public String phone;
        public String first_name;
        public String last_name;
        public TL_textWithEntities note;

        public static TL_inputPhoneContact TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_inputPhoneContact result = TL_inputPhoneContact.constructor != constructor ? null : new TL_inputPhoneContact();
            return TLdeserialize(TL_inputPhoneContact.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            client_id = stream.readInt64(exception);
            phone = stream.readString(exception);
            first_name = stream.readString(exception);
            last_name = stream.readString(exception);
            if (hasFlag(flags, FLAG_0)) {
                note = TL_textWithEntities.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            stream.writeInt64(client_id);
            stream.writeString(phone);
            stream.writeString(first_name);
            stream.writeString(last_name);
            if (hasFlag(flags, FLAG_0)) {
                note.serializeToStream(stream);
            }
        }
    }

    public static abstract class ThemeSettings extends TLObject {

        public int flags;
        public boolean message_colors_animated;
        public BaseTheme base_theme;
        public int accent_color;
        public int outbox_accent_color;
        public ArrayList<Integer> message_colors = new ArrayList<>();
        public WallPaper wallpaper;

        public static ThemeSettings TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            ThemeSettings result = null;
            switch (constructor) {
                case 0xfa58b6d4:
                    result = new TL_themeSettings();
                    break;
                case 0x8db4e76c:
                    result = new TL_themeSettings_layer132();
                    break;
                case 0x9c14984a:
                    result = new TL_themeSettings_layer131();
                    break;
            }
            return TLdeserialize(ThemeSettings.class, result, stream, constructor, exception);
        }
    }

    public static class TL_themeSettings extends ThemeSettings {
        public static final int constructor = 0xfa58b6d4;

        public static TL_themeSettings TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_themeSettings result = TL_themeSettings.constructor != constructor ? null : new TL_themeSettings();
            return TLdeserialize(TL_themeSettings.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            message_colors_animated = hasFlag(flags, FLAG_2);
            base_theme = BaseTheme.TLdeserialize(stream, stream.readInt32(exception), exception);
            accent_color = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_3)) {
                outbox_accent_color = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_0)) {
                message_colors = Vector.deserializeInt(stream, exception);
            }
            if (hasFlag(flags, FLAG_1)) {
                wallpaper = WallPaper.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_2, message_colors_animated);
            stream.writeInt32(flags);
            base_theme.serializeToStream(stream);
            stream.writeInt32(accent_color);
            if (hasFlag(flags, FLAG_3)) {
                stream.writeInt32(outbox_accent_color);
            }
            if (hasFlag(flags, FLAG_0)) {
                Vector.serializeInt(stream, message_colors);
            }
            if (hasFlag(flags, FLAG_1)) {
                wallpaper.serializeToStream(stream);
            }
        }
    }

    public static class TL_themeSettings_layer132 extends ThemeSettings {
        public static final int constructor = 0x8db4e76c;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            message_colors_animated = hasFlag(flags, FLAG_2);
            base_theme = BaseTheme.TLdeserialize(stream, stream.readInt32(exception), exception);
            accent_color = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_0)) {
                message_colors = Vector.deserializeInt(stream, exception);
            }
            if (hasFlag(flags, FLAG_1)) {
                wallpaper = WallPaper.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_2, message_colors_animated);
            stream.writeInt32(flags);
            base_theme.serializeToStream(stream);
            stream.writeInt32(accent_color);
            if (hasFlag(flags, FLAG_0)) {
                Vector.serializeInt(stream, message_colors);
            }
            if (hasFlag(flags, FLAG_1)) {
                wallpaper.serializeToStream(stream);
            }
        }
    }

    public static class TL_themeSettings_layer131 extends ThemeSettings {
        public static final int constructor = 0x9c14984a;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            base_theme = BaseTheme.TLdeserialize(stream, stream.readInt32(exception), exception);
            accent_color = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_0)) {
                int color = stream.readInt32(exception);
                if (color != 0) {
                    message_colors.add(color);
                }
            }
            if (hasFlag(flags, FLAG_0)) {
                int color = stream.readInt32(exception);
                if (color != 0) {
                    message_colors.add(0, color);
                }
            }
            if (hasFlag(flags, FLAG_1)) {
                wallpaper = WallPaper.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            base_theme.serializeToStream(stream);
            stream.writeInt32(accent_color);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeInt32(message_colors.size() > 1 ? message_colors.get(1) : 0);
            }
            if (hasFlag(flags, FLAG_0)) {
                stream.writeInt32(message_colors.size() > 0 ? message_colors.get(0) : 0);
            }
            if (hasFlag(flags, FLAG_1)) {
                wallpaper.serializeToStream(stream);
            }
        }
    }

    public static abstract class PrivacyRule extends TLObject {

        public static PrivacyRule TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            PrivacyRule result = null;
            switch (constructor) {
                case 0xf888fa1a:
                    result = new TL_privacyValueDisallowContacts();
                    break;
                case 0xe4621141:
                    result = new TL_privacyValueDisallowUsers();
                    break;
                case 0x6b134e8e:
                    result = new TL_privacyValueAllowChatParticipants();
                    break;
                case 0x41c87565:
                    result = new TL_privacyValueDisallowChatParticipants();
                    break;
                case 0x65427b82:
                    result = new TL_privacyValueAllowAll();
                    break;
                case 0x8b73e763:
                    result = new TL_privacyValueDisallowAll();
                    break;
                case 0xb8905fb2:
                    result = new TL_privacyValueAllowUsers();
                    break;
                case 0xfffe1bac:
                    result = new TL_privacyValueAllowContacts();
                    break;
                case 0xf7e8d89b:
                    result = new TL_privacyValueAllowCloseFriends();
                    break;
                case 0xece9814b:
                    result = new TL_privacyValueAllowPremium();
                    break;
                case TL_privacyValueAllowBots.constructor:
                    result = new TL_privacyValueAllowBots();
                    break;
                case TL_privacyValueDisallowBots.constructor:
                    result = new TL_privacyValueDisallowBots();
                    break;
            }
            return TLdeserialize(PrivacyRule.class, result, stream, constructor, exception);
        }
    }

    public static class TL_privacyValueDisallowContacts extends PrivacyRule {
        public static final int constructor = 0xf888fa1a;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_privacyValueDisallowUsers extends PrivacyRule {
        public static final int constructor = 0xe4621141;

        public ArrayList<Long> users = new ArrayList<>();

        public void readParams(InputSerializedData stream, boolean exception) {
            users = Vector.deserializeLong(stream, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            Vector.serializeLong(stream, users);
        }
    }

    public static class TL_privacyValueAllowChatParticipants extends PrivacyRule {
        public static final int constructor = 0x6b134e8e;

        public ArrayList<Long> chats = new ArrayList<>();

        public void readParams(InputSerializedData stream, boolean exception) {
            chats = Vector.deserializeLong(stream, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            Vector.serializeLong(stream, chats);
        }
    }

    public static class TL_privacyValueDisallowChatParticipants extends PrivacyRule {
        public static final int constructor = 0x41c87565;

        public ArrayList<Long> chats = new ArrayList<>();

        public void readParams(InputSerializedData stream, boolean exception) {
            chats = Vector.deserializeLong(stream, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            Vector.serializeLong(stream, chats);
        }
    }

    public static class TL_privacyValueAllowAll extends PrivacyRule {
        public static final int constructor = 0x65427b82;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_privacyValueDisallowAll extends PrivacyRule {
        public static final int constructor = 0x8b73e763;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_privacyValueAllowUsers extends PrivacyRule {
        public static final int constructor = 0xb8905fb2;

        public ArrayList<Long> users = new ArrayList<>();

        public void readParams(InputSerializedData stream, boolean exception) {
            users = Vector.deserializeLong(stream, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            Vector.serializeLong(stream, users);
        }
    }

    public static class TL_privacyValueAllowContacts extends PrivacyRule {
        public static final int constructor = 0xfffe1bac;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_privacyValueAllowCloseFriends extends PrivacyRule {
        public static final int constructor = 0xf7e8d89b;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_privacyValueAllowPremium extends PrivacyRule {
        public static final int constructor = 0xece9814b;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_privacyValueAllowBots extends PrivacyRule {
        public static final int constructor = 0x21461b5d;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_privacyValueDisallowBots extends PrivacyRule {
        public static final int constructor = 0xf6a5f82f;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_messageMediaUnsupported_old extends TL_messageMediaUnsupported {
        public static final int constructor = 0x29632a36;

        public void readParams(InputSerializedData stream, boolean exception) {
            bytes = stream.readByteArray(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeByteArray(bytes);
        }
    }

    public static class TL_messageMediaAudio_layer45 extends MessageMedia {
        public static final int constructor = 0xc6b68300;

        public void readParams(InputSerializedData stream, boolean exception) {
            audio_unused = Audio.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            audio_unused.serializeToStream(stream);
        }
    }

    public static class TL_messageMediaPhoto_old extends TL_messageMediaPhoto {
        public static final int constructor = 0xc8c45a2a;

        public void readParams(InputSerializedData stream, boolean exception) {
            photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            photo.serializeToStream(stream);
        }
    }

    public static class MessageExtendedMedia extends TLObject {

        public String attachPath; // custom
        public float downloadProgress, uploadProgress; // custom

        public static MessageExtendedMedia TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            MessageExtendedMedia result = null;
            switch (constructor) {
                case 0xad628cc8:
                    result = new TL_messageExtendedMediaPreview();
                    break;
                case 0xee479c64:
                    result = new TL_messageExtendedMedia();
                    break;
            }
            return TLdeserialize(MessageExtendedMedia.class, result, stream, constructor, exception);
        }

    }

    public static class TL_messageExtendedMediaPreview extends MessageExtendedMedia {
        public static final int constructor = 0xad628cc8;

        public int flags;
        public int w;
        public int h;
        public PhotoSize thumb;
        public int video_duration;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_0)) {
                w = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_0)) {
                h = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_1)) {
                thumb = PhotoSize.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                video_duration = stream.readInt32(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeInt32(w);
            }
            if (hasFlag(flags, FLAG_0)) {
                stream.writeInt32(h);
            }
            if (hasFlag(flags, FLAG_1)) {
                thumb.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(video_duration);
            }
        }
    }

    public static class TL_messageExtendedMedia extends MessageExtendedMedia {
        public static final int constructor = 0xee479c64;

        public MessageMedia media;

        public void readParams(InputSerializedData stream, boolean exception) {
            media = MessageMedia.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            media.serializeToStream(stream);
        }
    }

    public static class TL_messageMediaInvoice extends MessageMedia {
        public static final int constructor = 0xf6a548d3;

        public WebDocument webPhoto;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            shipping_address_requested = hasFlag(flags, FLAG_1);
            test = hasFlag(flags, FLAG_3);
            title = stream.readString(exception);
            description = stream.readString(exception);
            if (hasFlag(flags, FLAG_0)) {
                webPhoto = WebDocument.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                receipt_msg_id = stream.readInt32(exception);
            }
            currency = stream.readString(exception);
            total_amount = stream.readInt64(exception);
            start_param = stream.readString(exception);
            if (hasFlag(flags, FLAG_4)) {
                extended_media.clear();
                extended_media.add(MessageExtendedMedia.TLdeserialize(stream, stream.readInt32(exception), exception));
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_1, shipping_address_requested);
            flags = setFlag(flags, FLAG_3, test);
            stream.writeInt32(flags);
            stream.writeString(title);
            stream.writeString(description);
            if (hasFlag(flags, FLAG_0)) {
                webPhoto.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(receipt_msg_id);
            }
            stream.writeString(currency);
            stream.writeInt64(total_amount);
            stream.writeString(start_param);
            if (hasFlag(flags, FLAG_4)) {
                extended_media.get(0).serializeToStream(stream);
            }
        }
    }

    public static class TL_messageMediaInvoice_layer145 extends TL_messageMediaInvoice {
        public static final int constructor = 0x84551347;

        public WebDocument photo;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            shipping_address_requested = hasFlag(flags, FLAG_1);
            test = hasFlag(flags, FLAG_3);
            title = stream.readString(exception);
            description = stream.readString(exception);
            if (hasFlag(flags, FLAG_0)) {
                photo = WebDocument.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                receipt_msg_id = stream.readInt32(exception);
            }
            currency = stream.readString(exception);
            total_amount = stream.readInt64(exception);
            start_param = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_1, shipping_address_requested);
            flags = setFlag(flags, FLAG_3, test);
            stream.writeInt32(flags);
            stream.writeString(title);
            stream.writeString(description);
            if (hasFlag(flags, FLAG_0)) {
                photo.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(receipt_msg_id);
            }
            stream.writeString(currency);
            stream.writeInt64(total_amount);
            stream.writeString(start_param);
        }
    }

    public static class TL_messageMediaUnsupported extends MessageMedia {
        public static final int constructor = 0x9f84f49e;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_messageMediaEmpty extends MessageMedia {
        public static final int constructor = 0x3ded6320;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_messageMediaVenue extends MessageMedia {
        public static final int constructor = 0x2ec0533f;

        public String icon; //custom
        public String emoji; //custom
        public long query_id; //custom
        public String result_id; //custom
        public TL_stories.TL_geoPointAddress geoAddress; //custom

        public void readParams(InputSerializedData stream, boolean exception) {
            geo = GeoPoint.TLdeserialize(stream, stream.readInt32(exception), exception);
            title = stream.readString(exception);
            address = stream.readString(exception);
            provider = stream.readString(exception);
            venue_id = stream.readString(exception);
            venue_type = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            geo.serializeToStream(stream);
            stream.writeString(title);
            stream.writeString(address);
            stream.writeString(provider);
            stream.writeString(venue_id);
            stream.writeString(venue_type);
        }
    }

    public static class TL_messageMediaVenue_layer71 extends MessageMedia {
        public static final int constructor = 0x7912b71f;

        public void readParams(InputSerializedData stream, boolean exception) {
            geo = GeoPoint.TLdeserialize(stream, stream.readInt32(exception), exception);
            title = stream.readString(exception);
            address = stream.readString(exception);
            provider = stream.readString(exception);
            venue_id = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            geo.serializeToStream(stream);
            stream.writeString(title);
            stream.writeString(address);
            stream.writeString(provider);
            stream.writeString(venue_id);
        }
    }

    public static class TL_messageMediaVideo_old extends TL_messageMediaVideo_layer45 {
        public static final int constructor = 0xa2d24290;

        public void readParams(InputSerializedData stream, boolean exception) {
            video_unused = Video.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            video_unused.serializeToStream(stream);
        }
    }

    public static class TL_messageMediaDocument extends MessageMedia {
        public static final int constructor = 0x52d8ccd9;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            nopremium = hasFlag(flags, FLAG_3);
            spoiler = hasFlag(flags, FLAG_4);
            video = hasFlag(flags, FLAG_6);
            round = hasFlag(flags, FLAG_7);
            voice = hasFlag(flags, FLAG_8);
            live_photo = hasFlag(flags, FLAG_11);
            if (hasFlag(flags, FLAG_0)) {
                document = Document.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_5)) {
                alt_documents = Vector.deserialize(stream, Document::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_9)) {
                video_cover = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_10)) {
                video_timestamp = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                ttl_seconds = stream.readInt32(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_3, nopremium);
            flags = setFlag(flags, FLAG_4, spoiler);
            flags = setFlag(flags, FLAG_6, video);
            flags = setFlag(flags, FLAG_7, round);
            flags = setFlag(flags, FLAG_8, voice);
            flags = setFlag(flags, FLAG_11, live_photo);
            flags = setFlag(flags, FLAG_12, webpage != null);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_0)) {
                document.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_5)) {
                Vector.serialize(stream, alt_documents);
            }
            if (hasFlag(flags, FLAG_9)) {
                video_cover.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_10)) {
                stream.writeInt32(video_timestamp);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(ttl_seconds);
            }
        }
    }

    public static class TL_messageMediaDocument_layer197_2 extends TL_messageMediaDocument {
        public static final int constructor = 0xdbbdf614;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            nopremium = hasFlag(flags, FLAG_3);
            spoiler = hasFlag(flags, FLAG_4);
            video = hasFlag(flags, FLAG_6);
            round = hasFlag(flags, FLAG_7);
            voice = hasFlag(flags, FLAG_8);
            if (hasFlag(flags, FLAG_0)) {
                document = Document.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_5)) {
                alt_documents = Vector.deserialize(stream, Document::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_9)) {
                video_cover = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                ttl_seconds = stream.readInt32(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_3, nopremium);
            flags = setFlag(flags, FLAG_4, spoiler);
            flags = setFlag(flags, FLAG_6, video);
            flags = setFlag(flags, FLAG_7, round);
            flags = setFlag(flags, FLAG_8, voice);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_0)) {
                document.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_5)) {
                Vector.serialize(stream, alt_documents);
            }
            if (hasFlag(flags, FLAG_9)) {
                video_cover.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(ttl_seconds);
            }
        }
    }

    public static class TL_messageMediaDocument_layer197 extends TL_messageMediaDocument {
        public static final int constructor = 0xdd570bd5;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            nopremium = hasFlag(flags, FLAG_3);
            spoiler = hasFlag(flags, FLAG_4);
            video = hasFlag(flags, FLAG_6);
            round = hasFlag(flags, FLAG_7);
            voice = hasFlag(flags, FLAG_8);
            if (hasFlag(flags, FLAG_0)) {
                document = Document.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_5)) {
                alt_documents = Vector.deserialize(stream, Document::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                ttl_seconds = stream.readInt32(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_3, nopremium);
            flags = setFlag(flags, FLAG_4, spoiler);
            flags = setFlag(flags, FLAG_6, video);
            flags = setFlag(flags, FLAG_7, round);
            flags = setFlag(flags, FLAG_8, voice);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_0)) {
                document.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_5)) {
                Vector.serialize(stream, alt_documents);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(ttl_seconds);
            }
        }
    }

    public static class TL_messageMediaDocument_layer187 extends TL_messageMediaDocument {
        public static final int constructor = 0x4cf4d72d;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            nopremium = hasFlag(flags, FLAG_3);
            spoiler = hasFlag(flags, FLAG_4);
            video = hasFlag(flags, FLAG_6);
            round = hasFlag(flags, FLAG_7);
            voice = hasFlag(flags, FLAG_8);
            if (hasFlag(flags, FLAG_0)) {
                document = Document.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_5)) {
                alt_documents.add(Document.TLdeserialize(stream, stream.readInt32(exception), exception));
            }
            if (hasFlag(flags, FLAG_2)) {
                ttl_seconds = stream.readInt32(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_3, nopremium);
            flags = setFlag(flags, FLAG_4, spoiler);
            flags = setFlag(flags, FLAG_6, video);
            flags = setFlag(flags, FLAG_7, round);
            flags = setFlag(flags, FLAG_8, voice);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_0)) {
                document.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_5)) {
                alt_documents.get(0).serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(ttl_seconds);
            }
        }
    }

    public static class TL_messageMediaDocument_layer159 extends TL_messageMediaDocument {
        public static final int constructor = 0x9cb070d7;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            nopremium = hasFlag(flags, FLAG_3);
            spoiler = hasFlag(flags, FLAG_4);
            if (hasFlag(flags, FLAG_0)) {
                document = Document.TLdeserialize(stream, stream.readInt32(exception), exception);
            } else {
                document = new TL_documentEmpty();
            }
            if (hasFlag(flags, FLAG_2)) {
                ttl_seconds = stream.readInt32(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_3, nopremium);
            flags = setFlag(flags, FLAG_4, spoiler);
            flags = setFlag(flags, FLAG_0, document != null);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_0)) {
                document.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(ttl_seconds);
            }
        }
    }

    public static class TL_messageMediaDocument_layer74 extends TL_messageMediaDocument {
        public static final int constructor = 0x7c4414d3;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_0)) {
                document = Document.TLdeserialize(stream, stream.readInt32(exception), exception);
            } else {
                document = new TL_documentEmpty();
            }
            if (hasFlag(flags, FLAG_1)) {
                captionLegacy = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                ttl_seconds = stream.readInt32(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_0)) {
                document.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_1)) {
                stream.writeString(captionLegacy);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(ttl_seconds);
            }
        }
    }

    public static class TL_messageMediaDocument_old extends TL_messageMediaDocument {
        public static final int constructor = 0x2fda2204;

        public void readParams(InputSerializedData stream, boolean exception) {
            document = Document.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            document.serializeToStream(stream);
        }
    }

    public static class TL_messageMediaDocument_layer68 extends TL_messageMediaDocument {
        public static final int constructor = 0xf3e02ea8;

        public void readParams(InputSerializedData stream, boolean exception) {
            document = Document.TLdeserialize(stream, stream.readInt32(exception), exception);
            captionLegacy = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            document.serializeToStream(stream);
            stream.writeString(captionLegacy);
        }
    }

    public static class TL_messageMediaPhoto extends MessageMedia {
        public static final int constructor = 0xe216eb63;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            spoiler = hasFlag(flags, FLAG_3);
            live_photo = hasFlag(flags, FLAG_4);
            if (hasFlag(flags, FLAG_0)) {
                photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
            } else {
                photo = new TL_photoEmpty();
            }
            if (hasFlag(flags, FLAG_2)) {
                ttl_seconds = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                document = Document.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_3, spoiler);
            flags = setFlag(flags, FLAG_4, live_photo);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_0)) {
                photo.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(ttl_seconds);
            }
            if (hasFlag(flags, FLAG_4)) {
                document.serializeToStream(stream);
            }
        }
    }

    public static class TL_messageMediaPhoto_layer223 extends TL_messageMediaPhoto {
        public static final int constructor = 0x695150d7;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            spoiler = hasFlag(flags, FLAG_3);
            if (hasFlag(flags, FLAG_0)) {
                photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
            } else {
                photo = new TL_photoEmpty();
            }
            if (hasFlag(flags, FLAG_2)) {
                ttl_seconds = stream.readInt32(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_3, spoiler);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_0)) {
                photo.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(ttl_seconds);
            }
        }
    }

    public static class TL_messageMediaPoll extends MessageMedia {
        public static final int constructor = 0x773f4e66;

        public Poll poll;
        public PollResults results;
        public MessageMedia attached_media;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            poll = Poll.TLdeserialize(stream, exception);
            results = PollResults.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_0)) {
                attached_media = MessageMedia.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, attached_media != null);
            stream.writeInt32(flags);
            poll.serializeToStream(stream);
            results.serializeToStream(stream);
            if (hasFlag(flags, FLAG_0)) {
                attached_media.serializeToStream(stream);
            }
        }
    }

    public static class TL_messageMediaPoll_layer223 extends TL_messageMediaPoll {
        public static final int constructor = 0x4bd6e798;

        public void readParams(InputSerializedData stream, boolean exception) {
            poll = Poll.TLdeserialize(stream, exception);
            results = PollResults.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            poll.serializeToStream(stream);
            results.serializeToStream(stream);
        }
    }

    public static class TL_messageMediaPhoto_layer74 extends TL_messageMediaPhoto {
        public static final int constructor = 0xb5223b0f;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_0)) {
                photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
            } else {
                photo = new TL_photoEmpty();
            }
            if (hasFlag(flags, FLAG_1)) {
                captionLegacy = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                ttl_seconds = stream.readInt32(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_0)) {
                photo.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_1)) {
                stream.writeString(captionLegacy);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(ttl_seconds);
            }
        }
    }

    public static class TL_inputMediaInvoice extends InputMedia {
        public static final int constructor = 0x405fef0d;

        public int flags;
        public String title;
        public String description;
        public TL_inputWebDocument photo;
        public TL_invoice invoice;
        public byte[] payload;
        public String provider;
        public TL_dataJSON provider_data;
        public String start_param;
        public InputMedia extend_media;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            title = stream.readString(exception);
            description = stream.readString(exception);
            if (hasFlag(flags, FLAG_0)) {
                photo = TL_inputWebDocument.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            invoice = TL_invoice.TLdeserialize(stream, stream.readInt32(exception), exception);
            payload = stream.readByteArray(exception);
            if (hasFlag(flags, FLAG_3)) {
                provider = stream.readString(exception);
            }
            provider_data = TL_dataJSON.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_1)) {
                start_param = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                extend_media = InputMedia.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            stream.writeString(title);
            stream.writeString(description);
            if (hasFlag(flags, FLAG_0)) {
                photo.serializeToStream(stream);
            }
            invoice.serializeToStream(stream);
            stream.writeByteArray(payload);
            if (hasFlag(flags, FLAG_3)) {
                stream.writeString(provider);
            }
            provider_data.serializeToStream(stream);
            if (hasFlag(flags, FLAG_1)) {
                stream.writeString(start_param);
            }
            if (hasFlag(flags, FLAG_2)) {
                extend_media.serializeToStream(stream);
            }
        }
    }

    public static class TL_messageMediaGeoLive extends MessageMedia {
        public static final int constructor = 0xb940c666;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            geo = GeoPoint.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_0)) {
                heading = stream.readInt32(exception);
            }
            period = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_1)) {
                proximity_notification_radius = stream.readInt32(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            geo.serializeToStream(stream);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeInt32(heading);
            }
            stream.writeInt32(period);
            if (hasFlag(flags, FLAG_1)) {
                stream.writeInt32(proximity_notification_radius);
            }
        }
    }

    public static class TL_messageMediaGeoLive_layer119 extends TL_messageMediaGeoLive {
        public static final int constructor = 0x7c3c2609;

        public void readParams(InputSerializedData stream, boolean exception) {
            geo = GeoPoint.TLdeserialize(stream, stream.readInt32(exception), exception);
            period = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            geo.serializeToStream(stream);
            stream.writeInt32(period);
        }
    }

    public static class TL_messageMediaGame extends MessageMedia {
        public static final int constructor = 0xfdb19008;

        public void readParams(InputSerializedData stream, boolean exception) {
            game = TL_game.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            game.serializeToStream(stream);
        }
    }

    public static class TL_messageMediaContact_layer81 extends TL_messageMediaContact {
        public static final int constructor = 0x5e7d2f39;

        public void readParams(InputSerializedData stream, boolean exception) {
            phone_number = stream.readString(exception);
            first_name = stream.readString(exception);
            last_name = stream.readString(exception);
            user_id = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(phone_number);
            stream.writeString(first_name);
            stream.writeString(last_name);
            stream.writeInt32((int) user_id);
        }
    }

    public static class TL_messageMediaPhoto_layer68 extends TL_messageMediaPhoto {
        public static final int constructor = 0x3d8ce53d;

        public void readParams(InputSerializedData stream, boolean exception) {
            photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
            captionLegacy = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            photo.serializeToStream(stream);
            stream.writeString(captionLegacy);
        }
    }

    public static class TL_messageMediaVideo_layer45 extends MessageMedia {
        public static final int constructor = 0x5bcf1675;

        public void readParams(InputSerializedData stream, boolean exception) {
            video_unused = Video.TLdeserialize(stream, stream.readInt32(exception), exception);
            captionLegacy = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            video_unused.serializeToStream(stream);
            stream.writeString(captionLegacy);
        }
    }

    public static class TL_messageMediaContact_layer131 extends TL_messageMediaContact {
        public static final int constructor = 0xcbf24940;

        public void readParams(InputSerializedData stream, boolean exception) {
            phone_number = stream.readString(exception);
            first_name = stream.readString(exception);
            last_name = stream.readString(exception);
            vcard = stream.readString(exception);
            user_id = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(phone_number);
            stream.writeString(first_name);
            stream.writeString(last_name);
            stream.writeString(vcard);
            stream.writeInt32((int) user_id);
        }
    }

    public static class TL_messageMediaContact extends MessageMedia {
        public static final int constructor = 0x70322949;

        public void readParams(InputSerializedData stream, boolean exception) {
            phone_number = stream.readString(exception);
            first_name = stream.readString(exception);
            last_name = stream.readString(exception);
            vcard = stream.readString(exception);
            user_id = stream.readInt64(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(phone_number);
            stream.writeString(first_name);
            stream.writeString(last_name);
            stream.writeString(vcard);
            stream.writeInt64(user_id);
        }
    }

    public static class TL_messageMediaVideoStream extends MessageMedia {
        public static final int constructor = 0xca5cab89;

        public int flags;
        public InputGroupCall call;
        public boolean rtmp_stream;

        @Override
        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            rtmp_stream = hasFlag(flags, FLAG_0);
            call = InputGroupCall.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        @Override
        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, rtmp_stream);
            stream.writeInt32(flags);
            call.serializeToStream(stream);
        }
    }

    public static class TL_messageMediaDice extends MessageMedia {
        public static final int constructor = 0x8cbec07;

        public int value;
        public String emoticon;
        public TL_messages_emojiGameOutcome game_outcome;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            value = stream.readInt32(exception);
            emoticon = stream.readString(exception);
            if (hasFlag(flags, FLAG_0)) {
                game_outcome = TL_messages_emojiGameOutcome.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            stream.writeInt32(value);
            stream.writeString(emoticon);
            if (hasFlag(flags, FLAG_0)) {
                game_outcome.serializeToStream(stream);
            }
        }
    }

    public static class TL_messageMediaDice_layer220 extends TL_messageMediaDice {
        public static final int constructor = 0x3f7ee58b;

        public void readParams(InputSerializedData stream, boolean exception) {
            value = stream.readInt32(exception);
            emoticon = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(value);
            stream.writeString(emoticon);
        }
    }

    public static class TL_messageMediaDice_layer111 extends TL_messageMediaDice {
        public static final int constructor = 0x638fe46b;

        public void readParams(InputSerializedData stream, boolean exception) {
            value = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(value);
        }
    }

    public static class TL_messageMediaGeo extends MessageMedia {
        public static final int constructor = 0x56e0d474;

        public void readParams(InputSerializedData stream, boolean exception) {
            geo = GeoPoint.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            geo.serializeToStream(stream);
        }
    }

    public static class TL_messageMediaWebPage extends MessageMedia {
        public static final int constructor = 0xddf10c3b;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            force_large_media = hasFlag(flags, FLAG_0);
            force_small_media = hasFlag(flags, FLAG_1);
            manual = hasFlag(flags, FLAG_3);
            safe = hasFlag(flags, FLAG_4);
            webpage = WebPage.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, force_large_media);
            flags = setFlag(flags, FLAG_1, force_small_media);
            flags = setFlag(flags, FLAG_3, manual);
            flags = setFlag(flags, FLAG_4, safe);
            stream.writeInt32(flags);
            webpage.serializeToStream(stream);
        }
    }

    public static class TL_messageMediaWebPage_layer165 extends TL_messageMediaWebPage {
        public static final int constructor = 0xa32dd600;

        public void readParams(InputSerializedData stream, boolean exception) {
            webpage = WebPage.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            webpage.serializeToStream(stream);
        }
    }

    public static abstract class LangPackString extends TLObject {
        public int flags;
        public String key;
        public String zero_value;
        public String one_value;
        public String two_value;
        public String few_value;
        public String many_value;
        public String other_value;
        public String value;

        public static LangPackString TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            LangPackString result = null;
            switch (constructor) {
                case 0x6c47ac9f:
                    result = new TL_langPackStringPluralized();
                    break;
                case 0xcad181f6:
                    result = new TL_langPackString();
                    break;
                case 0x2979eeb2:
                    result = new TL_langPackStringDeleted();
                    break;
            }
            return TLdeserialize(LangPackString.class, result, stream, constructor, exception);
        }
    }

    public static class TL_langPackStringPluralized extends LangPackString {
        public static final int constructor = 0x6c47ac9f;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            key = stream.readString(exception);
            if (hasFlag(flags, FLAG_0)) {
                zero_value = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_1)) {
                one_value = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                two_value = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_3)) {
                few_value = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                many_value = stream.readString(exception);
            }
            other_value = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            stream.writeString(key);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeString(zero_value);
            }
            if (hasFlag(flags, FLAG_1)) {
                stream.writeString(one_value);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeString(two_value);
            }
            if (hasFlag(flags, FLAG_3)) {
                stream.writeString(few_value);
            }
            if (hasFlag(flags, FLAG_4)) {
                stream.writeString(many_value);
            }
            stream.writeString(other_value);
        }
    }

    public static class TL_langPackString extends LangPackString {
        public static final int constructor = 0xcad181f6;

        public void readParams(InputSerializedData stream, boolean exception) {
            key = stream.readString(exception);
            value = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(key);
            stream.writeString(value);
        }
    }

    public static class TL_langPackStringDeleted extends LangPackString {
        public static final int constructor = 0x2979eeb2;

        public void readParams(InputSerializedData stream, boolean exception) {
            key = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(key);
        }
    }

    public static abstract class auth_SentCode extends TLObject {

        public int flags;
        public auth_SentCodeType type;
        public String phone_code_hash;
        public auth_CodeType next_type;
        public int timeout;
        public auth_Authorization authorization;

        public static auth_SentCode TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            auth_SentCode result = null;
            switch (constructor) {
                case TL_auth_sentCodeSuccess.constructor:
                    result = new TL_auth_sentCodeSuccess();
                    break;
                case TL_auth_sentCode.constructor:
                    result = new TL_auth_sentCode();
                    break;
                case TL_auth_sentCodePaymentRequired.constructor:
                    result = new TL_auth_sentCodePaymentRequired();
                    break;
            }
            return TLdeserialize(auth_SentCode.class, result, stream, constructor, exception);
        }
    }

    public static class TL_auth_sentCodeSuccess extends auth_SentCode {
        public static final int constructor = 0x2390fe44;

        public void readParams(InputSerializedData stream, boolean exception) {
            authorization = auth_Authorization.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            authorization.serializeToStream(stream);
        }
    }

    public static class TL_auth_sentCode extends auth_SentCode {
        public static final int constructor = 0x5e002502;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            type = auth_SentCodeType.TLdeserialize(stream, stream.readInt32(exception), exception);
            phone_code_hash = stream.readString(exception);
            if (hasFlag(flags, FLAG_1)) {
                next_type = auth_CodeType.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                timeout = stream.readInt32(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            type.serializeToStream(stream);
            stream.writeString(phone_code_hash);
            if (hasFlag(flags, FLAG_1)) {
                next_type.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(timeout);
            }
        }
    }

    public static class TL_auth_sentCodePaymentRequired extends auth_SentCode {
        public static final int constructor = 0xf8827ebf;

        public String store_product;
        public String phone_code_hash;
        public String support_email_address;
        public String support_email_subject;
        public int premium_days;
        public String currency;
        public long amount;

        public void readParams(InputSerializedData stream, boolean exception) {
            store_product = stream.readString(exception);
            phone_code_hash = stream.readString(exception);
            support_email_address = stream.readString(exception);
            support_email_subject = stream.readString(exception);
            premium_days = stream.readInt32(exception);
            currency = stream.readString(exception);
            amount = stream.readInt64(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(store_product);
            stream.writeString(phone_code_hash);
            stream.writeString(support_email_address);
            stream.writeString(support_email_subject);
            stream.writeInt32(premium_days);
            stream.writeString(currency);
            stream.writeInt64(amount);
        }
    }

    public static abstract class BotInlineResult extends TLObject {

        public int flags;
        public String id;
        public String type;
        public Photo photo;
        public Document document;
        public String title;
        public String description;
        public String url;
        public WebDocument thumb;
        public WebDocument content;
        public BotInlineMessage send_message;
        public long query_id;

        public static BotInlineResult TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            BotInlineResult result = null;
            switch (constructor) {
                case 0x11965f3a:
                    result = new TL_botInlineResult();
                    break;
                case 0x17db940b:
                    result = new TL_botInlineMediaResult();
                    break;
            }
            return TLdeserialize(BotInlineResult.class, result, stream, constructor, exception);
        }
    }

    public static class TL_botInlineResult extends BotInlineResult {
        public static final int constructor = 0x11965f3a;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            id = stream.readString(exception);
            type = stream.readString(exception);
            if (hasFlag(flags, FLAG_1)) {
                title = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                description = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_3)) {
                url = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                thumb = WebDocument.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_5)) {
                content = WebDocument.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            send_message = BotInlineMessage.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            stream.writeString(id);
            stream.writeString(type);
            if (hasFlag(flags, FLAG_1)) {
                stream.writeString(title);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeString(description);
            }
            if (hasFlag(flags, FLAG_3)) {
                stream.writeString(url);
            }
            if (hasFlag(flags, FLAG_4)) {
                thumb.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_5)) {
                content.serializeToStream(stream);
            }
            send_message.serializeToStream(stream);
        }
    }

    public static class TL_botInlineMediaResult extends BotInlineResult {
        public static final int constructor = 0x17db940b;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            id = stream.readString(exception);
            type = stream.readString(exception);
            if (hasFlag(flags, FLAG_0)) {
                photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_1)) {
                document = Document.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                title = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_3)) {
                description = stream.readString(exception);
            }
            send_message = BotInlineMessage.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            stream.writeString(id);
            stream.writeString(type);
            if (hasFlag(flags, FLAG_0)) {
                photo.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_1)) {
                document.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeString(title);
            }
            if (hasFlag(flags, FLAG_3)) {
                stream.writeString(description);
            }
            send_message.serializeToStream(stream);
        }
    }

    public static class TL_notificationSoundDefault extends NotificationSound {
        public static final int constructor = 0x97e8bebe;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_notificationSoundNone extends NotificationSound {
        public static final int constructor = 0x6f0c34df;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_notificationSoundRingtone extends NotificationSound {
        public static final int constructor = 0xff6c8049;

        public long id;

        public void readParams(InputSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(id);
        }
    }

    public static class TL_notificationSoundLocal extends NotificationSound {
        public static final int constructor = 0x830b9ae4;

        public String title;
        public String data;

        public void readParams(InputSerializedData stream, boolean exception) {
            title = stream.readString(exception);
            data = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(title);
            stream.writeString(data);
        }
    }

    public static abstract class NotificationSound extends TLObject {

        public static NotificationSound TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            NotificationSound result = null;
            switch (constructor) {
                case 0x97e8bebe:
                    result = new TL_notificationSoundDefault();
                    break;
                case 0x6f0c34df:
                    result = new TL_notificationSoundNone();
                    break;
                case 0xff6c8049:
                    result = new TL_notificationSoundRingtone();
                    break;
                case 0x830b9ae4:
                    result = new TL_notificationSoundLocal();
                    break;
            }
            return TLdeserialize(NotificationSound.class, result, stream, constructor, exception);
        }
    }

    public static abstract class PeerNotifySettings extends TLObject {
        public int flags;
        public int mute_until;
        public String sound;
        public boolean show_previews;
        public int events_mask;
        public boolean silent;
        public NotificationSound ios_sound;
        public NotificationSound android_sound;
        public NotificationSound other_sound;
        public boolean stories_muted;
        public boolean stories_hide_sender;
        public NotificationSound stories_ios_sound;
        public NotificationSound stories_android_sound;
        public NotificationSound stories_other_sound;

        public static PeerNotifySettings TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            PeerNotifySettings result = null;
            switch (constructor) {
                case 0x99622c0c:
                    result = new TL_peerNotifySettings();
                    break;
                case 0xa83b0426:
                    result = new TL_peerNotifySettings_layer156();
                    break;
                case 0x9acda4c0:
                    result = new TL_peerNotifySettings_layer77();
                    break;
                case 0xaf509d20:
                    result = new TL_peerNotifySettings_layer139();
                    break;
                case 0x8d5e11ee:
                    result = new TL_peerNotifySettings_layer47();
                    break;
                case 0x70a68512:
                    result = new TL_peerNotifySettingsEmpty_layer77();
                    break;
            }
            return TLdeserialize(PeerNotifySettings.class, result, stream, constructor, exception);
        }
    }

    public static class TL_peerNotifySettings_layer77 extends TL_peerNotifySettings {
        public static final int constructor = 0x9acda4c0;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            show_previews = hasFlag(flags, FLAG_0);
            silent = hasFlag(flags, FLAG_1);
            mute_until = stream.readInt32(exception);
            sound = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, show_previews);
            flags = setFlag(flags, FLAG_1, silent);
            stream.writeInt32(flags);
            stream.writeInt32(mute_until);
            stream.writeString(sound);
        }
    }

    public static class TL_peerNotifySettings extends PeerNotifySettings {
        public static final int constructor = 0x99622c0c;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_0)) {
                show_previews = stream.readBool(exception);
            }
            if (hasFlag(flags, FLAG_1)) {
                silent = stream.readBool(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                mute_until = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_3)) {
                ios_sound = NotificationSound.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                android_sound = NotificationSound.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_5)) {
                other_sound = NotificationSound.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_6)) {
                stories_muted = stream.readBool(exception);
            }
            if (hasFlag(flags, FLAG_7)) {
                stories_hide_sender = stream.readBool(exception);
            }
            if (hasFlag(flags, FLAG_8)) {
                stories_ios_sound = NotificationSound.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_9)) {
                stories_android_sound = NotificationSound.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_10)) {
                stories_other_sound = NotificationSound.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeBool(show_previews);
            }
            if (hasFlag(flags, FLAG_1)) {
                stream.writeBool(silent);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(mute_until);
            }
            if (hasFlag(flags, FLAG_3)) {
                ios_sound.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_4)) {
                android_sound.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_5)) {
                other_sound.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_6)) {
                stream.writeBool(stories_muted);
            }
            if (hasFlag(flags, FLAG_7)) {
                stream.writeBool(stories_hide_sender);
            }
            if (hasFlag(flags, FLAG_8)) {
                stories_ios_sound.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_9)) {
                stories_android_sound.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_10)) {
                stories_other_sound.serializeToStream(stream);
            }
        }
    }

    public static class TL_peerNotifySettings_layer156 extends TL_peerNotifySettings {
        public static final int constructor = 0xa83b0426;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_0)) {
                show_previews = stream.readBool(exception);
            }
            if (hasFlag(flags, FLAG_1)) {
                silent = stream.readBool(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                mute_until = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_3)) {
                ios_sound = NotificationSound.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                android_sound = NotificationSound.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_5)) {
                other_sound = NotificationSound.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeBool(show_previews);
            }
            if (hasFlag(flags, FLAG_1)) {
                stream.writeBool(silent);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(mute_until);
            }
            if (hasFlag(flags, FLAG_3)) {
                ios_sound.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_4)) {
                android_sound.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_5)) {
                other_sound.serializeToStream(stream);
            }
        }
    }

    public static class TL_peerNotifySettings_layer139 extends TL_peerNotifySettings {
        public static final int constructor = 0xaf509d20;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_0)) {
                show_previews = stream.readBool(exception);
            }
            if (hasFlag(flags, FLAG_1)) {
                silent = stream.readBool(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                mute_until = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_3)) {
                sound = stream.readString(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeBool(show_previews);
            }
            if (hasFlag(flags, FLAG_1)) {
                stream.writeBool(silent);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(mute_until);
            }
            if (hasFlag(flags, FLAG_3)) {
                stream.writeString(sound);
            }
        }
    }

    public static class TL_peerNotifySettings_layer47 extends TL_peerNotifySettings {
        public static final int constructor = 0x8d5e11ee;

        public void readParams(InputSerializedData stream, boolean exception) {
            mute_until = stream.readInt32(exception);
            sound = stream.readString(exception);
            show_previews = stream.readBool(exception);
            events_mask = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(mute_until);
            stream.writeString(sound);
            stream.writeBool(show_previews);
            stream.writeInt32(events_mask);
        }
    }

    public static class TL_peerNotifySettingsEmpty_layer77 extends PeerNotifySettings {
        public static final int constructor = 0x70a68512;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static abstract class contacts_Blocked extends TLObject {

        public ArrayList<TL_peerBlocked> blocked = new ArrayList<>();
        public ArrayList<Chat> chats = new ArrayList<>();
        public ArrayList<User> users = new ArrayList<>();
        public int count;

        public static contacts_Blocked TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            contacts_Blocked result = null;
            switch (constructor) {
                case 0xade1591:
                    result = new TL_contacts_blocked();
                    break;
                case 0xe1664194:
                    result = new TL_contacts_blockedSlice();
                    break;
            }
            return TLdeserialize(contacts_Blocked.class, result, stream, constructor, exception);
        }
    }

    public static class TL_contacts_blocked extends contacts_Blocked {
        public static final int constructor = 0xade1591;

        public void readParams(InputSerializedData stream, boolean exception) {
            blocked = Vector.deserialize(stream, TL_peerBlocked::TLdeserialize, exception);
            chats = Vector.deserialize(stream, Chat::TLdeserialize, exception);
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            Vector.serialize(stream, blocked);
            Vector.serialize(stream, chats);
            Vector.serialize(stream, users);
        }
    }

    public static class TL_contacts_blockedSlice extends contacts_Blocked {
        public static final int constructor = 0xe1664194;

        public void readParams(InputSerializedData stream, boolean exception) {
            count = stream.readInt32(exception);
            blocked = Vector.deserialize(stream, TL_peerBlocked::TLdeserialize, exception);
            chats = Vector.deserialize(stream, Chat::TLdeserialize, exception);
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(count);
            Vector.serialize(stream, blocked);
            Vector.serialize(stream, chats);
            Vector.serialize(stream, users);
        }
    }

    public static class TL_inputSecureValue extends TLObject {
        public static final int constructor = 0xdb21d0a7;

        public int flags;
        public SecureValueType type;
        public TL_secureData data;
        public InputSecureFile front_side;
        public InputSecureFile reverse_side;
        public InputSecureFile selfie;
        public ArrayList<InputSecureFile> translation = new ArrayList<>();
        public ArrayList<InputSecureFile> files = new ArrayList<>();
        public SecurePlainData plain_data;

        public static TL_inputSecureValue TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_inputSecureValue result = TL_inputSecureValue.constructor != constructor ? null : new TL_inputSecureValue();
            return TLdeserialize(TL_inputSecureValue.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            type = SecureValueType.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_0)) {
                data = TL_secureData.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_1)) {
                front_side = InputSecureFile.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                reverse_side = InputSecureFile.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_3)) {
                selfie = InputSecureFile.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_6)) {
                translation = Vector.deserialize(stream, InputSecureFile::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                files = Vector.deserialize(stream, InputSecureFile::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_5)) {
                plain_data = SecurePlainData.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            type.serializeToStream(stream);
            if (hasFlag(flags, FLAG_0)) {
                data.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_1)) {
                front_side.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_2)) {
                reverse_side.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_3)) {
                selfie.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_6)) {
                Vector.serialize(stream, translation);
            }
            if (hasFlag(flags, FLAG_4)) {
                Vector.serialize(stream, files);
            }
            if (hasFlag(flags, FLAG_5)) {
                plain_data.serializeToStream(stream);
            }
        }
    }

    public static abstract class help_AppConfig extends TLObject {

        public static help_AppConfig TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            help_AppConfig result = null;
            switch (constructor) {
                case 0xdd18782e:
                    result = new TL_help_appConfig();
                    break;
                case 0x7cde641d:
                    result = new TL_help_appConfigNotModified();
                    break;
            }
            return TLdeserialize(help_AppConfig.class, result, stream, constructor, exception);
        }
    }

    public static class TL_help_appConfig extends help_AppConfig {
        public static final int constructor = 0xdd18782e;

        public int hash;
        public JSONValue config;

        public void readParams(InputSerializedData stream, boolean exception) {
            hash = stream.readInt32(exception);
            config = JSONValue.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(hash);
            config.serializeToStream(stream);
        }
    }

    public static class TL_help_appConfigNotModified extends help_AppConfig {
        public static final int constructor = 0x7cde641d;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static abstract class messages_DhConfig extends TLObject {
        public byte[] random;
        public int g;
        public byte[] p;
        public int version;

        public static messages_DhConfig TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            messages_DhConfig result = null;
            switch (constructor) {
                case 0xc0e24635:
                    result = new TL_messages_dhConfigNotModified();
                    break;
                case 0x2c221edd:
                    result = new TL_messages_dhConfig();
                    break;
            }
            return TLdeserialize(messages_DhConfig.class, result, stream, constructor, exception);
        }
    }

    public static class TL_messages_dhConfigNotModified extends messages_DhConfig {
        public static final int constructor = 0xc0e24635;

        public void readParams(InputSerializedData stream, boolean exception) {
            random = stream.readByteArray(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeByteArray(random);
        }
    }

    public static class TL_messages_dhConfig extends messages_DhConfig {
        public static final int constructor = 0x2c221edd;

        public void readParams(InputSerializedData stream, boolean exception) {
            g = stream.readInt32(exception);
            p = stream.readByteArray(exception);
            version = stream.readInt32(exception);
            random = stream.readByteArray(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(g);
            stream.writeByteArray(p);
            stream.writeInt32(version);
            stream.writeByteArray(random);
        }
    }
    
    public static class DisallowedGiftsSettings extends TLObject {
        public static final int constructor = 0x71f276c4;
        
        public int flags;
        public boolean disallow_unlimited_stargifts;
        public boolean disallow_limited_stargifts;
        public boolean disallow_unique_stargifts;
        public boolean disallow_premium_gifts;
        public boolean disallow_stargifts_from_channels;

        public static DisallowedGiftsSettings TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final DisallowedGiftsSettings result = DisallowedGiftsSettings.constructor != constructor ? null : new DisallowedGiftsSettings();
            return TLdeserialize(DisallowedGiftsSettings.class, result, stream, constructor, exception);
        }

        @Override
        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            disallow_unlimited_stargifts = hasFlag(flags, FLAG_0);
            disallow_limited_stargifts = hasFlag(flags, FLAG_1);
            disallow_unique_stargifts = hasFlag(flags, FLAG_2);
            disallow_premium_gifts = hasFlag(flags, FLAG_3);
            disallow_stargifts_from_channels = hasFlag(flags, FLAG_4);
        }

        @Override
        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, disallow_unlimited_stargifts);
            flags = setFlag(flags, FLAG_1, disallow_limited_stargifts);
            flags = setFlag(flags, FLAG_2, disallow_unique_stargifts);
            flags = setFlag(flags, FLAG_3, disallow_premium_gifts);
            flags = setFlag(flags, FLAG_4, disallow_stargifts_from_channels);
            stream.writeInt32(flags);
        }
    }

    public static class GlobalPrivacySettings extends TLObject {

        public int flags;
        public boolean archive_and_mute_new_noncontact_peers;
        public boolean keep_archived_unmuted;
        public boolean keep_archived_folders;
        public boolean hide_read_marks;
        public boolean new_noncontact_peers_require_premium;
        public boolean display_gifts_button;
        public long noncontact_peers_paid_stars;
        public DisallowedGiftsSettings disallowed_stargifts;

        public static GlobalPrivacySettings TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            GlobalPrivacySettings result = null;
            switch (constructor) {
                case TL_globalPrivacySettings.constructor:
                    result = new TL_globalPrivacySettings();
                    break;
                case TL_globalPrivacySettings_layer200.constructor:
                    result = new TL_globalPrivacySettings_layer200();
                    break;
            }
            return TLdeserialize(GlobalPrivacySettings.class, result, stream, constructor, exception);
        }
    }

    public static class TL_globalPrivacySettings extends GlobalPrivacySettings {
        public static final int constructor = 0xfe41b34f;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            archive_and_mute_new_noncontact_peers = hasFlag(flags, FLAG_0);
            keep_archived_unmuted = hasFlag(flags, FLAG_1);
            keep_archived_folders = hasFlag(flags, FLAG_2);
            hide_read_marks = hasFlag(flags, FLAG_3);
            new_noncontact_peers_require_premium = hasFlag(flags, FLAG_4);
            display_gifts_button = hasFlag(flags, FLAG_7);
            if (hasFlag(flags, FLAG_5)) {
                noncontact_peers_paid_stars = stream.readInt64(exception);
            }
            if (hasFlag(flags, FLAG_6)) {
                disallowed_stargifts = DisallowedGiftsSettings.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, archive_and_mute_new_noncontact_peers);
            flags = setFlag(flags, FLAG_1, keep_archived_unmuted);
            flags = setFlag(flags, FLAG_2, keep_archived_folders);
            flags = setFlag(flags, FLAG_3, hide_read_marks);
            flags = setFlag(flags, FLAG_4, new_noncontact_peers_require_premium);
            flags = setFlag(flags, FLAG_7, display_gifts_button);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_5)) {
                stream.writeInt64(noncontact_peers_paid_stars);
            }
            if (hasFlag(flags, FLAG_6)) {
                disallowed_stargifts.serializeToStream(stream);
            }
        }
    }

    public static class TL_globalPrivacySettings_layer200 extends TL_globalPrivacySettings {
        public static final int constructor = 0xc9d8df1c;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            archive_and_mute_new_noncontact_peers = hasFlag(flags, FLAG_0);
            keep_archived_unmuted = hasFlag(flags, FLAG_1);
            keep_archived_folders = hasFlag(flags, FLAG_2);
            hide_read_marks = hasFlag(flags, FLAG_3);
            new_noncontact_peers_require_premium = hasFlag(flags, FLAG_4);
            if (hasFlag(flags, FLAG_5)) {
                noncontact_peers_paid_stars = stream.readInt64(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, archive_and_mute_new_noncontact_peers);
            flags = setFlag(flags, FLAG_1, keep_archived_unmuted);
            flags = setFlag(flags, FLAG_2, keep_archived_folders);
            flags = setFlag(flags, FLAG_3, hide_read_marks);
            flags = setFlag(flags, FLAG_4, new_noncontact_peers_require_premium);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_5)) {
                stream.writeInt64(noncontact_peers_paid_stars);
            }
        }
    }

    public static class TL_help_premiumPromo_layer144 extends TL_help_premiumPromo {
        public static final int constructor = 0x8a4f3c29;

        public void readParams(InputSerializedData stream, boolean exception) {
            status_text = stream.readString(exception);
            status_entities = Vector.deserialize(stream, MessageEntity::TLdeserialize, exception);
            video_sections = Vector.deserializeString(stream, exception);
            videos = Vector.deserialize(stream, Document::TLdeserialize, exception);
            currency = stream.readString(exception);
            monthly_amount = stream.readInt64(exception);
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(status_text);
            Vector.serialize(stream, status_entities);
            Vector.serializeString(stream, video_sections);
            Vector.serialize(stream, videos);
            stream.writeString(currency);
            stream.writeInt64(monthly_amount);
            Vector.serialize(stream, users);
        }
    }

    public static class TL_help_premiumPromo_layer140 extends TL_help_premiumPromo {
        public static final int constructor = 0xe0360f1b;

        public void readParams(InputSerializedData stream, boolean exception) {
            status_text = stream.readString(exception);
            status_entities = Vector.deserialize(stream, MessageEntity::TLdeserialize, exception);
            video_sections = Vector.deserializeString(stream, exception);
            videos = Vector.deserialize(stream, Document::TLdeserialize, exception);
            currency = stream.readString(exception);
            monthly_amount = stream.readInt64(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(status_text);
            Vector.serialize(stream, status_entities);
            Vector.serializeString(stream, video_sections);
            Vector.serialize(stream, videos);
            stream.writeString(currency);
            stream.writeInt64(monthly_amount);
        }
    }

    public static class TL_help_premiumPromo extends TLObject {
        public static final int constructor = 0x5334759c;

        public String status_text;
        public ArrayList<MessageEntity> status_entities = new ArrayList<>();
        public ArrayList<String> video_sections = new ArrayList<>();
        public ArrayList<Document> videos = new ArrayList<>();
        public ArrayList<TL_premiumSubscriptionOption> period_options = new ArrayList<>();
        public ArrayList<User> users = new ArrayList<>();
        public String currency;
        public long monthly_amount;

        public static TL_help_premiumPromo TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            TL_help_premiumPromo result = null;
            switch (constructor) {
                case 0x5334759c:
                    result = new TL_help_premiumPromo();
                    break;
                case 0x8a4f3c29:
                    result = new TL_help_premiumPromo_layer144();
                    break;
                case 0xe0360f1b:
                    result = new TL_help_premiumPromo_layer140();
                    break;
            }
            result = TLdeserialize(TL_help_premiumPromo.class, result, stream, constructor, exception);
            if (result != null && result.currency != null) {
                TL_help_premiumPromo finalResult = result;
                result.period_options.add(new TL_premiumSubscriptionOption() {{
                    months = 1;
                    currency = finalResult.currency;
                    amount = finalResult.monthly_amount;
                    store_product = "telegram_premium";
                }});
            }
            return result;
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            status_text = stream.readString(exception);
            status_entities = Vector.deserialize(stream, MessageEntity::TLdeserialize, exception);
            video_sections = Vector.deserializeString(stream, exception);
            videos = Vector.deserialize(stream, Document::TLdeserialize, exception);
            period_options = Vector.deserialize(stream, TL_premiumSubscriptionOption::TLdeserialize, exception);
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(status_text);
            Vector.serialize(stream, status_entities);
            Vector.serializeString(stream, video_sections);
            Vector.serialize(stream, videos);
            Vector.serialize(stream, period_options);
            Vector.serialize(stream, users);
        }
    }

    public static abstract class help_UserInfo extends TLObject {

        public static help_UserInfo TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            help_UserInfo result = null;
            switch (constructor) {
                case 0xf3ae2eed:
                    result = new TL_help_userInfoEmpty();
                    break;
                case 0x1eb3758:
                    result = new TL_help_userInfo();
                    break;
            }
            return TLdeserialize(help_UserInfo.class, result, stream, constructor, exception);
        }
    }

    public static class TL_help_userInfoEmpty extends help_UserInfo {
        public static final int constructor = 0xf3ae2eed;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_help_userInfo extends help_UserInfo {
        public static final int constructor = 0x1eb3758;

        public String message;
        public ArrayList<MessageEntity> entities = new ArrayList<>();
        public String author;
        public int date;

        public void readParams(InputSerializedData stream, boolean exception) {
            message = stream.readString(exception);
            entities = Vector.deserialize(stream, MessageEntity::TLdeserialize, exception);
            author = stream.readString(exception);
            date = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(message);
            Vector.serialize(stream, entities);
            stream.writeString(author);
            stream.writeInt32(date);
        }
    }

    public static class TL_secureValueHash extends TLObject {
        public static final int constructor = 0xed1ecdb0;

        public SecureValueType type;
        public byte[] hash;

        public static TL_secureValueHash TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_secureValueHash result = TL_secureValueHash.constructor != constructor ? null : new TL_secureValueHash();
            return TLdeserialize(TL_secureValueHash.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            type = SecureValueType.TLdeserialize(stream, stream.readInt32(exception), exception);
            hash = stream.readByteArray(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            type.serializeToStream(stream);
            stream.writeByteArray(hash);
        }
    }

    public static abstract class messages_StickerSet extends TLObject {

        public StickerSet set;
        public ArrayList<TL_stickerPack> packs = new ArrayList<>();
        public ArrayList<TL_stickerKeyword> keywords = new ArrayList<>();
        public ArrayList<Document> documents = new ArrayList<>();

        public static TL_messages_stickerSet TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            TL_messages_stickerSet result = null;
            switch (constructor) {
                case 0x6e153f16:
                    result = new TL_messages_stickerSet();
                    break;
                case 0xb60a24a6:
                    result = new TL_messages_stickerSet_layer146();
                    break;
                case 0xd3f924eb:
                    result = new TL_messages_stickerSetNotModified();
                    break;
            }
            return TLdeserialize(TL_messages_stickerSet.class, result, stream, constructor, exception);
        }
    }

    public static class TL_messages_stickerSet_layer146 extends TL_messages_stickerSet {
        public static final int constructor = 0xb60a24a6;

        public void readParams(InputSerializedData stream, boolean exception) {
            set = StickerSet.TLdeserialize(stream, stream.readInt32(exception), exception);
            packs = Vector.deserialize(stream, TL_stickerPack::TLdeserialize, exception);
            documents = Vector.deserialize(stream, Document::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            set.serializeToStream(stream);
            Vector.serialize(stream, packs);
            Vector.serialize(stream, documents);
        }
    }

    public static class TL_messages_stickerSet extends messages_StickerSet {
        public static final int constructor = 0x6e153f16;

        public void readParams(InputSerializedData stream, boolean exception) {
            set = StickerSet.TLdeserialize(stream, stream.readInt32(exception), exception);
            packs = Vector.deserialize(stream, TL_stickerPack::TLdeserialize, exception);
            keywords = Vector.deserialize(stream, TL_stickerKeyword::TLdeserialize, exception);
            documents = Vector.deserialize(stream, Document::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            set.serializeToStream(stream);
            Vector.serialize(stream, packs);
            Vector.serialize(stream, keywords);
            Vector.serialize(stream, documents);
        }
    }

    public static class TL_messages_stickerSetNotModified extends TL_messages_stickerSet {
        public static final int constructor = 0xd3f924eb;

        public void readParams(InputSerializedData stream, boolean exception) {

        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static abstract class InputGeoPoint extends TLObject {

        public int flags;
        public double lat;
        public double _long;
        public int accuracy_radius;

        public static InputGeoPoint TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            InputGeoPoint result = null;
            switch (constructor) {
                case 0x48222faf:
                    result = new TL_inputGeoPoint();
                    break;
                case 0xe4c123d6:
                    result = new TL_inputGeoPointEmpty();
                    break;
            }
            return TLdeserialize(InputGeoPoint.class, result, stream, constructor, exception);
        }
    }

    public static class TL_inputGeoPoint extends InputGeoPoint {
        public static final int constructor = 0x48222faf;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            lat = stream.readDouble(exception);
            _long = stream.readDouble(exception);
            if (hasFlag(flags, FLAG_0)) {
                accuracy_radius = stream.readInt32(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            stream.writeDouble(lat);
            stream.writeDouble(_long);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeInt32(accuracy_radius);
            }
        }
    }

    public static class TL_inputGeoPointEmpty extends InputGeoPoint {
        public static final int constructor = 0xe4c123d6;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_help_inviteText extends TLObject {
        public static final int constructor = 0x18cb9f78;

        public String message;

        public static TL_help_inviteText TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_help_inviteText result = TL_help_inviteText.constructor != constructor ? null : new TL_help_inviteText();
            return TLdeserialize(TL_help_inviteText.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            message = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(message);
        }
    }

    public static abstract class Audio extends TLObject {
        public long id;
        public long access_hash;
        public int date;
        public int duration;
        public String mime_type;
        public int size;
        public int dc_id;
        public long user_id;
        public byte[] key;
        public byte[] iv;

        public static Audio TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            Audio result = null;
            switch (constructor) {
                case 0x586988d8:
                    result = new TL_audioEmpty_layer45();
                    break;
                case 0xf9e35055:
                    result = new TL_audio_layer45();
                    break;
                case 0x427425e7:
                    result = new TL_audio_old();
                    break;
                case 0x555555F6:
                    result = new TL_audioEncrypted();
                    break;
                case 0xc7ac6496:
                    result = new TL_audio_old2();
                    break;
            }
            return TLdeserialize(Audio.class, result, stream, constructor, exception);
        }
    }

    public static class TL_audioEmpty_layer45 extends Audio {
        public static final int constructor = 0x586988d8;

        public void readParams(InputSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(id);
        }
    }

    public static class TL_audio_layer45 extends Audio {
        public static final int constructor = 0xf9e35055;

        public void readParams(InputSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
            access_hash = stream.readInt64(exception);
            date = stream.readInt32(exception);
            duration = stream.readInt32(exception);
            mime_type = stream.readString(exception);
            size = stream.readInt32(exception);
            dc_id = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(id);
            stream.writeInt64(access_hash);
            stream.writeInt32(date);
            stream.writeInt32(duration);
            stream.writeString(mime_type);
            stream.writeInt32(size);
            stream.writeInt32(dc_id);
        }
    }

    public static class TL_audio_old extends TL_audio_layer45 {
        public static final int constructor = 0x427425e7;

        public void readParams(InputSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
            access_hash = stream.readInt64(exception);
            user_id = stream.readInt32(exception);
            date = stream.readInt32(exception);
            duration = stream.readInt32(exception);
            size = stream.readInt32(exception);
            dc_id = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(id);
            stream.writeInt64(access_hash);
            stream.writeInt32((int) user_id);
            stream.writeInt32(date);
            stream.writeInt32(duration);
            stream.writeInt32(size);
            stream.writeInt32(dc_id);
        }
    }

    public static class TL_audioEncrypted extends TL_audio_layer45 {
        public static final int constructor = 0x555555F6;

        public void readParams(InputSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
            access_hash = stream.readInt64(exception);
            user_id = stream.readInt32(exception);
            date = stream.readInt32(exception);
            duration = stream.readInt32(exception);
            size = stream.readInt32(exception);
            dc_id = stream.readInt32(exception);
            key = stream.readByteArray(exception);
            iv = stream.readByteArray(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(id);
            stream.writeInt64(access_hash);
            stream.writeInt32((int) user_id);
            stream.writeInt32(date);
            stream.writeInt32(duration);
            stream.writeInt32(size);
            stream.writeInt32(dc_id);
            stream.writeByteArray(key);
            stream.writeByteArray(iv);
        }
    }

    public static class TL_audio_old2 extends TL_audio_layer45 {
        public static final int constructor = 0xc7ac6496;

        public void readParams(InputSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
            access_hash = stream.readInt64(exception);
            user_id = stream.readInt32(exception);
            date = stream.readInt32(exception);
            duration = stream.readInt32(exception);
            mime_type = stream.readString(exception);
            size = stream.readInt32(exception);
            dc_id = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(id);
            stream.writeInt64(access_hash);
            stream.writeInt32((int) user_id);
            stream.writeInt32(date);
            stream.writeInt32(duration);
            stream.writeString(mime_type);
            stream.writeInt32(size);
            stream.writeInt32(dc_id);
        }
    }

    public static class TL_help_country extends TLObject {
        public static final int constructor = 0xc3878e23;

        public int flags;
        public boolean hidden;
        public String iso2;
        public String default_name;
        public String name;
        public ArrayList<TL_help_countryCode> country_codes = new ArrayList<>();

        public static TL_help_country TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_help_country result = TL_help_country.constructor != constructor ? null : new TL_help_country();
            return TLdeserialize(TL_help_country.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            hidden = hasFlag(flags, FLAG_0);
            iso2 = stream.readString(exception);
            default_name = stream.readString(exception);
            if (hasFlag(flags, FLAG_1)) {
                name = stream.readString(exception);
            }
            country_codes = Vector.deserialize(stream, TL_help_countryCode::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, hidden);
            stream.writeInt32(flags);
            stream.writeString(iso2);
            stream.writeString(default_name);
            if (hasFlag(flags, FLAG_1)) {
                stream.writeString(name);
            }
            Vector.serialize(stream, country_codes);
        }
    }

    public static abstract class SecurePasswordKdfAlgo extends TLObject {

        public static SecurePasswordKdfAlgo TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            SecurePasswordKdfAlgo result = null;
            switch (constructor) {
                case 0xbbf2dda0:
                    result = new TL_securePasswordKdfAlgoPBKDF2HMACSHA512iter100000();
                    break;
                case 0x86471d92:
                    result = new TL_securePasswordKdfAlgoSHA512();
                    break;
                case 0x4a8537:
                    result = new TL_securePasswordKdfAlgoUnknown();
                    break;
            }
            return TLdeserialize(SecurePasswordKdfAlgo.class, result, stream, constructor, exception);
        }
    }

    public static class TL_securePasswordKdfAlgoPBKDF2HMACSHA512iter100000 extends SecurePasswordKdfAlgo {
        public static final int constructor = 0xbbf2dda0;

        public byte[] salt;

        public void readParams(InputSerializedData stream, boolean exception) {
            salt = stream.readByteArray(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeByteArray(salt);
        }
    }

    public static class TL_securePasswordKdfAlgoSHA512 extends SecurePasswordKdfAlgo {
        public static final int constructor = 0x86471d92;

        public byte[] salt;

        public void readParams(InputSerializedData stream, boolean exception) {
            salt = stream.readByteArray(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeByteArray(salt);
        }
    }

    public static class TL_securePasswordKdfAlgoUnknown extends SecurePasswordKdfAlgo {
        public static final int constructor = 0x4a8537;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_messages_historyImport extends TLObject {
        public static final int constructor = 0x1662af0b;

        public long id;

        public static TL_messages_historyImport TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_messages_historyImport result = TL_messages_historyImport.constructor != constructor ? null : new TL_messages_historyImport();
            return TLdeserialize(TL_messages_historyImport.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(id);
        }
    }

    public static abstract class InputGame extends TLObject {
        public InputUser bot_id;
        public String short_name;
        public long id;
        public long access_hash;

        public static InputGame TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            InputGame result = null;
            switch (constructor) {
                case 0xc331e80a:
                    result = new TL_inputGameShortName();
                    break;
                case 0x32c3e77:
                    result = new TL_inputGameID();
                    break;
            }
            return TLdeserialize(InputGame.class, result, stream, constructor, exception);
        }
    }

    public static class TL_inputGameShortName extends InputGame {
        public static final int constructor = 0xc331e80a;

        public void readParams(InputSerializedData stream, boolean exception) {
            bot_id = InputUser.TLdeserialize(stream, stream.readInt32(exception), exception);
            short_name = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            bot_id.serializeToStream(stream);
            stream.writeString(short_name);
        }
    }

    public static class TL_inputGameID extends InputGame {
        public static final int constructor = 0x32c3e77;

        public void readParams(InputSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
            access_hash = stream.readInt64(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(id);
            stream.writeInt64(access_hash);
        }
    }

    public static abstract class MessageReplies extends TLObject {

        public int flags;
        public boolean comments;
        public int replies;
        public int replies_pts;
        public ArrayList<Peer> recent_repliers = new ArrayList<>();
        public long channel_id;
        public int max_id;
        public int read_max_id;

        public static MessageReplies TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            MessageReplies result = null;
            switch (constructor) {
                case 0x4128faac:
                    result = new TL_messageReplies_layer131();
                    break;
                case 0x83d60fc2:
                    result = new TL_messageReplies();
                    break;
            }
            return TLdeserialize(MessageReplies.class, result, stream, constructor, exception);
        }
    }

    public static class TL_messageReplies_layer131 extends TL_messageReplies {
        public static final int constructor = 0x4128faac;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            comments = hasFlag(flags, FLAG_0);
            replies = stream.readInt32(exception);
            replies_pts = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_1)) {
                recent_repliers = Vector.deserialize(stream, Peer::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_0)) {
                channel_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                max_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_3)) {
                read_max_id = stream.readInt32(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, comments);
            stream.writeInt32(flags);
            stream.writeInt32(replies);
            stream.writeInt32(replies_pts);
            if (hasFlag(flags, FLAG_1)) {
                Vector.serialize(stream, recent_repliers);
            }
            if (hasFlag(flags, FLAG_0)) {
                stream.writeInt32((int) channel_id);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(max_id);
            }
            if (hasFlag(flags, FLAG_3)) {
                stream.writeInt32(read_max_id);
            }
        }
    }

    public static class TL_messageReplies extends MessageReplies {
        public static final int constructor = 0x83d60fc2;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            comments = hasFlag(flags, FLAG_0);
            replies = stream.readInt32(exception);
            replies_pts = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_1)) {
                recent_repliers = Vector.deserialize(stream, Peer::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_0)) {
                channel_id = stream.readInt64(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                max_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_3)) {
                read_max_id = stream.readInt32(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, comments);
            stream.writeInt32(flags);
            stream.writeInt32(replies);
            stream.writeInt32(replies_pts);
            if (hasFlag(flags, FLAG_1)) {
                Vector.serialize(stream, recent_repliers);
            }
            if (hasFlag(flags, FLAG_0)) {
                stream.writeInt64(channel_id);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(max_id);
            }
            if (hasFlag(flags, FLAG_3)) {
                stream.writeInt32(read_max_id);
            }
        }
    }

    public static abstract class messages_SponsoredMessages extends TLObject {
        public int flags;
        public int posts_between;
        public int start_delay;
        public int between_delay;
        public ArrayList<TL_sponsoredMessage> messages = new ArrayList<>();
        public ArrayList<Chat> chats = new ArrayList<>();
        public ArrayList<User> users = new ArrayList<>();

        public static messages_SponsoredMessages TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            messages_SponsoredMessages result = null;
            switch (constructor) {
                case TL_messages_sponsoredMessagesEmpty.constructor:
                    result = new TL_messages_sponsoredMessagesEmpty();
                    break;
                case TL_messages_sponsoredMessages.constructor:
                    result = new TL_messages_sponsoredMessages();
                    break;
            }
            return TLdeserialize(messages_SponsoredMessages.class, result, stream, constructor, exception);
        }
    }

    public static class TL_messages_sponsoredMessagesEmpty extends messages_SponsoredMessages {
        public static final int constructor = 0x1839490f;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_messages_sponsoredMessages extends messages_SponsoredMessages {
        public static final int constructor = 0xffda656d;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_0)) {
                posts_between = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_1)) {
                start_delay = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                between_delay = stream.readInt32(exception);
            }
            messages = Vector.deserialize(stream, TL_sponsoredMessage::TLdeserialize, exception);
            chats = Vector.deserialize(stream, Chat::TLdeserialize, exception);
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeInt32(posts_between);
            }
            if (hasFlag(flags, FLAG_1)) {
                stream.writeInt32(start_delay);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(between_delay);
            }
            Vector.serialize(stream, messages);
            Vector.serialize(stream, chats);
            Vector.serialize(stream, users);
        }
    }

    public static class TL_messageViews extends TLObject {
        public static final int constructor = 0x455b853d;

        public int flags;
        public int views;
        public int forwards;
        public MessageReplies replies;

        public static TL_messageViews TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_messageViews result = TL_messageViews.constructor != constructor ? null : new TL_messageViews();
            return TLdeserialize(TL_messageViews.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_0)) {
                views = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_1)) {
                forwards = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                replies = MessageReplies.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeInt32(views);
            }
            if (hasFlag(flags, FLAG_1)) {
                stream.writeInt32(forwards);
            }
            if (hasFlag(flags, FLAG_2)) {
                replies.serializeToStream(stream);
            }
        }
    }

    public static abstract class ReplyMarkup extends TLObject {
        public int flags;
        public boolean resize;
        public boolean single_use;
        public boolean is_persistent;
        public boolean selective;
        public boolean force_reply;
        public String placeholder;

        public static ReplyMarkup TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            return TLdeserialize(ReplyMarkup.class, fromConstructor(constructor), stream, constructor, exception);
        }

        private static ReplyMarkup fromConstructor(int constructor) {
            switch (constructor) {
                case TL_replyKeyboardMarkup.constructor:
                    return new TL_replyKeyboardMarkup();
                case TL_replyKeyboardHide.constructor:
                    return new TL_replyKeyboardHide();
                case TL_replyKeyboardForceReply.constructor:
                    return new TL_replyKeyboardForceReply();
                case TL_replyKeyboardMarkup_layer129.constructor:
                    return new TL_replyKeyboardMarkup_layer129();
                case TL_replyKeyboardForceReply_layer129.constructor:
                    return new TL_replyKeyboardForceReply_layer129();
                case TL_replyInlineMarkup.constructor:
                    return new TL_replyInlineMarkup();
                case TL_replyInlineMarkup_layer228_old.constructor:
                    return new TL_replyInlineMarkup_layer228_old();
                case TL_replyInlineMarkup_layer228.constructor:
                    return new TL_replyInlineMarkup_layer228();
                default:
                    return null;
            }
        }
    }

    public static class TL_replyKeyboardMarkup extends ReplyMarkup {
        public static final int constructor = 0x85dd99d1;

        public ArrayList<TL_keyboard.KeyboardButtonRow> rows = new ArrayList<>();

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            resize = hasFlag(flags, FLAG_0);
            single_use = hasFlag(flags, FLAG_1);
            selective = hasFlag(flags, FLAG_2);
            is_persistent = hasFlag(flags, FLAG_4);
            force_reply = hasFlag(flags, FLAG_5);
            rows = Vector.deserialize(stream, TL_keyboard.KeyboardButtonRow::TLdeserialize, exception);
            if (hasFlag(flags, FLAG_3)) {
                placeholder = stream.readString(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, resize);
            flags = setFlag(flags, FLAG_1, single_use);
            flags = setFlag(flags, FLAG_2, selective);
            flags = setFlag(flags, FLAG_4, is_persistent);
            flags = setFlag(flags, FLAG_5, force_reply);
            stream.writeInt32(flags);
            Vector.serialize(stream, rows);
            if (hasFlag(flags, FLAG_3)) {
                stream.writeString(placeholder);
            }
        }
    }

    public static class TL_replyKeyboardHide extends ReplyMarkup {
        public static final int constructor = 0xa03e5b85;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            selective = hasFlag(flags, FLAG_2);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_2, selective);
            stream.writeInt32(flags);
        }
    }

    public static class TL_replyKeyboardForceReply extends ReplyMarkup {
        public static final int constructor = 0x86b40b08;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            single_use = hasFlag(flags, FLAG_1);
            selective = hasFlag(flags, FLAG_2);
            if (hasFlag(flags, FLAG_3)) {
                placeholder = stream.readString(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_1, single_use);
            flags = setFlag(flags, FLAG_2, selective);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_3)) {
                stream.writeString(placeholder);
            }
        }
    }

    public static class TL_replyKeyboardMarkup_layer129 extends TL_replyKeyboardMarkup {
        public static final int constructor = 0x3502758c;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            resize = hasFlag(flags, FLAG_0);
            single_use = hasFlag(flags, FLAG_1);
            selective = hasFlag(flags, FLAG_2);
            rows = Vector.deserialize(stream, TL_keyboard.KeyboardButtonRow::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, resize);
            flags = setFlag(flags, FLAG_1, single_use);
            flags = setFlag(flags, FLAG_2, selective);
            stream.writeInt32(flags);
            Vector.serialize(stream, rows);
        }
    }

    public static class TL_replyKeyboardForceReply_layer129 extends TL_replyKeyboardForceReply {
        public static final int constructor = 0xf4108aa0;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            single_use = hasFlag(flags, FLAG_1);
            selective = hasFlag(flags, FLAG_2);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_1, single_use);
            flags = setFlag(flags, FLAG_2, selective);
            stream.writeInt32(flags);
        }
    }

    public static class TL_replyInlineMarkup extends ReplyMarkup {
        public static final int constructor = 0xB2B15770;

        public ArrayList<TL_keyboard.KeyboardInlineButtonRow> rows = new ArrayList<>();

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            force_reply = hasFlag(flags, FLAG_5);
            rows = Vector.deserialize(stream, TL_keyboard.KeyboardInlineButtonRow::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_5, force_reply);
            stream.writeInt32(flags);
            Vector.serialize(stream, rows);
        }
    }

    public static class TL_replyInlineMarkup_layer228_old extends TL_replyInlineMarkup {
        public static final int constructor = 0x58F7FCB6;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            force_reply = hasFlag(flags, FLAG_5);
            rows = Vector.deserialize(stream, TL_keyboard.KeyboardInlineButtonRow::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_5, force_reply);
            stream.writeInt32(flags);
            Vector.serialize(stream, rows);
        }
    }

    public static class TL_replyInlineMarkup_layer228 extends TL_replyInlineMarkup {
        public static final int constructor = 0x48a30254;

        public void readParams(InputSerializedData stream, boolean exception) {
            rows = Vector.deserialize(stream, TL_keyboard.KeyboardInlineButtonRow::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            Vector.serialize(stream, rows);
        }
    }

    public static class WebPageAttribute extends TLObject {
        public int flags;

        private static WebPageAttribute fromConstructor(int constructor) {
            switch (constructor) {
                case TL_webPageAttributeTheme.constructor:              return new TL_webPageAttributeTheme();
                case TL_webPageAttributeStory_layer162.constructor:     return new TL_webPageAttributeStory_layer162();
                case TL_webPageAttributeStory.constructor:              return new TL_webPageAttributeStory();
                case TL_webPageAttributeStickerSet.constructor:         return new TL_webPageAttributeStickerSet();
                case TL_webPageAttributeUniqueStarGift.constructor:     return new TL_webPageAttributeUniqueStarGift();
                case TL_webPageAttributeStarGiftCollection.constructor: return new TL_webPageAttributeStarGiftCollection();
                case TL_webPageAttributeStarGiftAuction.constructor:    return new TL_webPageAttributeStarGiftAuction();
                case TL_webPageAttributeStarGiftAuction_layer219.constructor: return new TL_webPageAttributeStarGiftAuction_layer219();
                case TL_webPageAttributeAiComposeTone.constructor:      return new TL_webPageAttributeAiComposeTone();
            }
            return null;
        }

        public static WebPageAttribute TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            return TLdeserialize(WebPageAttribute.class, fromConstructor(constructor), stream, constructor, exception);
        }
    }

    public static class TL_webPageAttributeStarGiftAuction extends WebPageAttribute {
        public final static int constructor = 0x01C641C2;

        public TL_stars.StarGift gift;
        public int end_date;

        @Deprecated
        public int center_color;

        @Deprecated
        public int edge_color;

        @Deprecated
        public int text_color;

        @Override
        public void readParams(InputSerializedData stream, boolean exception) {
            gift = TL_stars.StarGift.TLdeserialize(stream, stream.readInt32(exception), exception);
            end_date = stream.readInt32(exception);

            if (gift != null && gift.background != null) {
                center_color = gift.background.center_color;
                edge_color = gift.background.edge_color;
                text_color = gift.background.text_color;
            }
        }

        @Override
        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            gift.serializeToStream(stream);
            stream.writeInt32(end_date);
        }
    }

    public static class TL_webPageAttributeAiComposeTone extends WebPageAttribute {
        public final static int constructor = 0x7781fe18;

        public long emoji_id;

        @Override
        public void readParams(InputSerializedData stream, boolean exception) {
            emoji_id = stream.readInt64(exception);
        }

        @Override
        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(emoji_id);
        }
    }

    public static class TL_webPageAttributeStarGiftAuction_layer219 extends TL_webPageAttributeStarGiftAuction {
        public final static int constructor = 0x34986ab;

        @Override
        public void readParams(InputSerializedData stream, boolean exception) {
            gift = TL_stars.StarGift.TLdeserialize(stream, stream.readInt32(exception), exception);
            end_date = stream.readInt32(exception);
            center_color = stream.readInt32(exception);
            edge_color = stream.readInt32(exception);
            text_color = stream.readInt32(exception);
        }

        @Override
        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            gift.serializeToStream(stream);
            stream.writeInt32(end_date);
            stream.writeInt32(center_color);
            stream.writeInt32(edge_color);
            stream.writeInt32(text_color);
        }
    }

    public static class TL_webPageAttributeStory extends WebPageAttribute {
        public final static int constructor = 0x2e94c3e7;

        public Peer peer;
        public int id;
        public TL_stories.StoryItem storyItem;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            peer = Peer.TLdeserialize(stream, stream.readInt32(exception), exception);
            id = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_0)) {
                storyItem = TL_stories.StoryItem.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            peer.serializeToStream(stream);
            stream.writeInt32(id);
            if (hasFlag(flags, FLAG_0)) {
                storyItem.serializeToStream(stream);
            }
        }
    }

    public static class TL_webPageAttributeStickerSet extends WebPageAttribute {
        public final static int constructor = 0x50cc03d3;

        public Peer peer;
        public boolean emojis;
        public boolean text_color;
        public ArrayList<TLRPC.Document> stickers = new ArrayList<>();

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            emojis = hasFlag(flags, FLAG_0);
            text_color = hasFlag(flags, FLAG_1);
            stickers = Vector.deserialize(stream, Document::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, emojis);
            flags = setFlag(flags, FLAG_1, text_color);
            stream.writeInt32(flags);
            Vector.serialize(stream, stickers);
        }
    }

    public static class TL_webPageAttributeStarGiftCollection extends WebPageAttribute {
        public final static int constructor = 0x31cad303;

        public ArrayList<TLRPC.Document> icons = new ArrayList<>();

        public void readParams(InputSerializedData stream, boolean exception) {
            icons = Vector.deserialize(stream, TLRPC.Document::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            Vector.serialize(stream, icons);
        }
    }

    public static class TL_webPageAttributeUniqueStarGift extends WebPageAttribute {
        public final static int constructor = 0xcf6f6db8;

        public TL_stars.StarGift gift;

        public void readParams(InputSerializedData stream, boolean exception) {
            gift = TL_stars.StarGift.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            gift.serializeToStream(stream);
        }
    }

    public static class TL_webPageAttributeStory_layer162 extends TL_webPageAttributeStory {
        public static final int constructor = 0x939a4671;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            long user_id = stream.readInt64(exception);
            peer = new TL_peerUser();
            peer.user_id = user_id;
            id = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_0)) {
                storyItem = TL_stories.StoryItem.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            if (storyItem != null) {
                flags |= 1;
            } else {
                flags &= ~1;
            }
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            stream.writeInt64(peer.user_id);
            stream.writeInt32(id);
            if (hasFlag(flags, FLAG_0)) {
                storyItem.serializeToStream(stream);
            }
        }
    }

    public static class TL_webPageAttributeTheme extends WebPageAttribute {
        public static final int constructor = 0x54b56617;

        public ArrayList<Document> documents = new ArrayList<>();
        public ThemeSettings settings;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_0)) {
                documents = Vector.deserialize(stream, Document::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_1)) {
                settings = ThemeSettings.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_0)) {
                Vector.serialize(stream, documents);
            }
            if (hasFlag(flags, FLAG_1)) {
                settings.serializeToStream(stream);
            }
        }
    }

    public static abstract class contacts_Contacts extends TLObject {
        public ArrayList<TL_contact> contacts = new ArrayList<>();
        public int saved_count;
        public ArrayList<User> users = new ArrayList<>();

        public static contacts_Contacts TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            contacts_Contacts result = null;
            switch (constructor) {
                case 0xb74ba9d2:
                    result = new TL_contacts_contactsNotModified();
                    break;
                case 0xeae87e42:
                    result = new TL_contacts_contacts();
                    break;
            }
            return TLdeserialize(contacts_Contacts.class, result, stream, constructor, exception);
        }
    }

    public static class TL_contacts_contactsNotModified extends contacts_Contacts {
        public static final int constructor = 0xb74ba9d2;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_contacts_contacts extends contacts_Contacts {
        public static final int constructor = 0xeae87e42;

        public void readParams(InputSerializedData stream, boolean exception) {
            contacts = Vector.deserialize(stream, TL_contact::TLdeserialize, exception);
            saved_count = stream.readInt32(exception);
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            Vector.serialize(stream, contacts);
            stream.writeInt32(saved_count);
            Vector.serialize(stream, users);
        }
    }

    public static abstract class SecureRequiredType extends TLObject {

        public static SecureRequiredType TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            SecureRequiredType result = null;
            switch (constructor) {
                case 0x829d99da:
                    result = new TL_secureRequiredType();
                    break;
                case 0x27477b4:
                    result = new TL_secureRequiredTypeOneOf();
                    break;
            }
            return TLdeserialize(SecureRequiredType.class, result, stream, constructor, exception);
        }
    }

    public static class TL_secureRequiredType extends SecureRequiredType {
        public static final int constructor = 0x829d99da;

        public int flags;
        public boolean native_names;
        public boolean selfie_required;
        public boolean translation_required;
        public SecureValueType type;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            native_names = hasFlag(flags, FLAG_0);
            selfie_required = hasFlag(flags, FLAG_1);
            translation_required = hasFlag(flags, FLAG_2);
            type = SecureValueType.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, native_names);
            flags = setFlag(flags, FLAG_1, selfie_required);
            flags = setFlag(flags, FLAG_2, translation_required);
            stream.writeInt32(flags);
            type.serializeToStream(stream);
        }
    }

    public static class TL_secureRequiredTypeOneOf extends SecureRequiredType {
        public static final int constructor = 0x27477b4;

        public ArrayList<SecureRequiredType> types = new ArrayList<>();

        public void readParams(InputSerializedData stream, boolean exception) {
            types = Vector.deserialize(stream, SecureRequiredType::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            Vector.serialize(stream, types);
        }
    }

    public static abstract class InputPrivacyKey extends TLObject {

        public static InputPrivacyKey TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            InputPrivacyKey result = null;
            switch (constructor) {
                case TL_inputPrivacyKeyStatusTimestamp.constructor:
                    result = new TL_inputPrivacyKeyStatusTimestamp();
                    break;
                case TL_inputPrivacyKeyChatInvite.constructor:
                    result = new TL_inputPrivacyKeyChatInvite();
                    break;
                case TL_inputPrivacyKeyPhoneCall.constructor:
                    result = new TL_inputPrivacyKeyPhoneCall();
                    break;
                case TL_inputPrivacyKeyForwards.constructor:
                    result = new TL_inputPrivacyKeyForwards();
                    break;
                case TL_inputPrivacyKeyProfilePhoto.constructor:
                    result = new TL_inputPrivacyKeyProfilePhoto();
                    break;
                case TL_inputPrivacyKeyPhoneNumber.constructor:
                    result = new TL_inputPrivacyKeyPhoneNumber();
                    break;
                case TL_inputPrivacyKeyAddedByPhone.constructor:
                    result = new TL_inputPrivacyKeyAddedByPhone();
                    break;
                case TL_inputPrivacyKeyVoiceMessages.constructor:
                    result = new TL_inputPrivacyKeyVoiceMessages();
                    break;
                case TL_inputPrivacyKeyPhoneP2P.constructor:
                    result = new TL_inputPrivacyKeyPhoneP2P();
                    break;
                case TL_inputPrivacyKeyAbout.constructor:
                    result = new TL_inputPrivacyKeyAbout();
                    break;
                case TL_inputPrivacyKeyBirthday.constructor:
                    result = new TL_inputPrivacyKeyBirthday();
                    break;
                case TL_inputPrivacyKeyStarGiftsAutoSave.constructor:
                    result = new TL_inputPrivacyKeyStarGiftsAutoSave();
                    break;
                case TL_inputPrivacyKeyNoPaidMessages.constructor:
                    result = new TL_inputPrivacyKeyNoPaidMessages();
                    break;
                case TL_inputPrivacyKeySavedMusic.constructor:
                    result = new TL_inputPrivacyKeySavedMusic();
                    break;
            }
            return TLdeserialize(InputPrivacyKey.class, result, stream, constructor, exception);
        }
    }

    public static class TL_inputPrivacyKeyStatusTimestamp extends InputPrivacyKey {
        public static final int constructor = 0x4f96cb18;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_inputPrivacyKeyChatInvite extends InputPrivacyKey {
        public static final int constructor = 0xbdfb0426;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_inputPrivacyKeyPhoneCall extends InputPrivacyKey {
        public static final int constructor = 0xfabadc5f;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_inputPrivacyKeyForwards extends InputPrivacyKey {
        public static final int constructor = 0xa4dd4c08;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_inputPrivacyKeyPhoneNumber extends InputPrivacyKey {
        public static final int constructor = 0x352dafa;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_inputPrivacyKeyAddedByPhone extends InputPrivacyKey {
        public static final int constructor = 0xd1219bdd;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_inputPrivacyKeyVoiceMessages extends InputPrivacyKey {
        public static final int constructor = 0xaee69d68;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_inputPrivacyKeyAbout extends InputPrivacyKey {
        public static final int constructor = 0x3823cc40;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_inputPrivacyKeyBirthday extends InputPrivacyKey {
        public static final int constructor = 0xd65a11cc;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_inputPrivacyKeyStarGiftsAutoSave extends InputPrivacyKey {
        public static final int constructor = 0xe1732341;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_inputPrivacyKeyNoPaidMessages extends InputPrivacyKey {
        public static final int constructor = 0xbdc597b4;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_inputPrivacyKeySavedMusic extends InputPrivacyKey {
        public static final int constructor = 0x4dbe9226;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_inputPrivacyKeyProfilePhoto extends InputPrivacyKey {
        public static final int constructor = 0x5719bacc;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_inputPrivacyKeyPhoneP2P extends InputPrivacyKey {
        public static final int constructor = 0xdb9e70d2;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static abstract class messages_ExportedChatInvite extends TLObject {

        public ExportedChatInvite invite;
        public ArrayList<User> users = new ArrayList<>();

        public static messages_ExportedChatInvite TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            messages_ExportedChatInvite result = null;
            switch (constructor) {
                case 0x222600ef:
                    result = new TL_messages_exportedChatInviteReplaced();
                    break;
                case 0x1871be50:
                    result = new TL_messages_exportedChatInvite();
                    break;
            }
            return TLdeserialize(messages_ExportedChatInvite.class, result, stream, constructor, exception);
        }
    }

    public static class TL_messages_exportedChatInviteReplaced extends messages_ExportedChatInvite {
        public static final int constructor = 0x222600ef;

        public ExportedChatInvite new_invite;

        public void readParams(InputSerializedData stream, boolean exception) {
            invite = ExportedChatInvite.TLdeserialize(stream, stream.readInt32(exception), exception);
            new_invite = ExportedChatInvite.TLdeserialize(stream, stream.readInt32(exception), exception);
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            invite.serializeToStream(stream);
            new_invite.serializeToStream(stream);
            Vector.serialize(stream, users);
        }
    }

    public static class TL_messages_exportedChatInvite extends messages_ExportedChatInvite {
        public static final int constructor = 0x1871be50;

        public void readParams(InputSerializedData stream, boolean exception) {
            invite = ExportedChatInvite.TLdeserialize(stream, stream.readInt32(exception), exception);
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            invite.serializeToStream(stream);
            Vector.serialize(stream, users);
        }
    }

    public static abstract class InputTheme extends TLObject {

        public static InputTheme TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            InputTheme result = null;
            switch (constructor) {
                case 0xf5890df1:
                    result = new TL_inputThemeSlug();
                    break;
                case 0x3c5693e9:
                    result = new TL_inputTheme();
                    break;
            }
            return TLdeserialize(InputTheme.class, result, stream, constructor, exception);
        }
    }

    public static class TL_inputThemeSlug extends InputTheme {
        public static final int constructor = 0xf5890df1;

        public String slug;

        public void readParams(InputSerializedData stream, boolean exception) {
            slug = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(slug);
        }
    }

    public static class TL_inputTheme extends InputTheme {
        public static final int constructor = 0x3c5693e9;

        public long id;
        public long access_hash;

        public void readParams(InputSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
            access_hash = stream.readInt64(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(id);
            stream.writeInt64(access_hash);
        }
    }

    public static abstract class photos_Photos extends TLObject {
        public ArrayList<Photo> photos = new ArrayList<>();
        public ArrayList<User> users = new ArrayList<>();
        public int count;

        public static photos_Photos TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            photos_Photos result = null;
            switch (constructor) {
                case 0x8dca6aa5:
                    result = new TL_photos_photos();
                    break;
                case 0x15051f54:
                    result = new TL_photos_photosSlice();
                    break;
            }
            return TLdeserialize(photos_Photos.class, result, stream, constructor, exception);
        }
    }

    public static class TL_photos_photos extends photos_Photos {
        public static final int constructor = 0x8dca6aa5;

        public void readParams(InputSerializedData stream, boolean exception) {
            photos = Vector.deserialize(stream, Photo::TLdeserialize, exception);
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            Vector.serialize(stream, photos);
            Vector.serialize(stream, users);
        }
    }

    public static class TL_photos_photosSlice extends photos_Photos {
        public static final int constructor = 0x15051f54;

        public void readParams(InputSerializedData stream, boolean exception) {
            count = stream.readInt32(exception);
            photos = Vector.deserialize(stream, Photo::TLdeserialize, exception);
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(count);
            Vector.serialize(stream, photos);
            Vector.serialize(stream, users);
        }
    }

    public static abstract class ChatFull extends TLObject {
        public long id;
        public ChatParticipants participants;
        public Photo chat_photo;
        public PeerNotifySettings notify_settings;
        public TL_chatInviteExported exported_invite;
        public ArrayList<TL_bots.BotInfo> bot_info = new ArrayList<>();
        public int flags;
        public boolean can_view_participants;
        public boolean can_set_username;
        public boolean has_scheduled;
        public String about;
        public int participants_count;
        public int admins_count;
        public int read_inbox_max_id;
        public int read_outbox_max_id;
        public int unread_count;
        public long migrated_from_chat_id;
        public int migrated_from_max_id;
        public int pinned_msg_id;
        public int kicked_count;
        public int unread_important_count;
        public int folder_id;
        public boolean can_set_stickers;
        public boolean hidden_prehistory;
        public boolean can_view_stats;
        public boolean can_set_location;
        public boolean blocked;
        public int banned_count;
        public int online_count;
        public StickerSet stickerset;
        public int available_min_id;
        public int call_msg_id;
        public long linked_chat_id;
        public ChannelLocation location;
        public int slowmode_seconds;
        public int slowmode_next_send_date;
        public int stats_dc;
        public int pts;
        public InputGroupCall call;
        public int ttl_period;
        public ArrayList<String> pending_suggestions = new ArrayList<>();
        public Peer groupcall_default_join_as;
        public ArrayList<Long> recent_requesters = new ArrayList<>();
        public String theme_emoticon;
        public int requests_pending;
        public Peer default_send_as;
        public ArrayList<String> available_reactions_legacy = new ArrayList<>();
        public int flags2;
        public boolean can_delete_channel;
        public boolean antispam;
        public boolean participants_hidden;
        public boolean translations_disabled;
        public boolean stories_pinned_available;
        public boolean view_forum_as_messages;
        public boolean restricted_sponsored;
        public ChatReactions available_reactions;
        public int reactions_limit;
        public TL_stories.PeerStories stories;
        public WallPaper wallpaper;
        public int boosts_applied;
        public int boosts_unrestrict;
        public StickerSet emojiset;
        public boolean can_view_revenue;
        public boolean can_view_stars_revenue;
        public boolean paid_media_allowed;
        public boolean paid_reactions_available;
        public boolean stargifts_available;
        public boolean paid_messages_available;
        public TL_bots.botVerification bot_verification;
        public int stargifts_count;
        public long send_paid_messages_stars;
        public ProfileTab main_tab;
        public long guard_bot_id;
        public boolean has_welcome_messages;
        public ArrayList<TL_communities.CommunityPeer> linked_peers = new ArrayList<>();
        public long inviterId; //custom
        public int invitesCount; //custom

        private static ChatFull fromConstructor(int constructor) {
            switch (constructor) {
                case TL_chatFull.constructor:
                    return new TL_chatFull();
                case TL_communityFull.constructor:
                    return new TL_communityFull();
                case TL_channelFull.constructor:
                    return new TL_channelFull();
                case TL_channelFull_layer225.constructor:
                    return new TL_channelFull_layer225();
                case TL_channelFull_layer212.constructor:
                    return new TL_channelFull_layer212();
                case TL_channelFull_layer204.constructor:
                    return new TL_channelFull_layer204();
                case TL_channelFull_layer197.constructor:
                    return new TL_channelFull_layer197();
                case TL_channelFull_layer195.constructor:
                    return new TL_channelFull_layer195();
                case TL_chatFull_layer177.constructor:
                    return new TL_chatFull_layer177();
                case TL_channelFull_layer177.constructor:
                    return new TL_channelFull_layer177();
                case TL_channelFull_layer176.constructor:
                    return new TL_channelFull_layer176();
                case TL_channelFull_layer167.constructor:
                    return new TL_channelFull_layer167();
                case TL_channelFull_layer173.constructor:
                    return new TL_channelFull_layer173();
                case TL_channelFull_layer162.constructor:
                    return new TL_channelFull_layer162();
                case TL_chatFull_layer144.constructor:
                    return new TL_chatFull_layer144();
                case TL_channelFull_layer144.constructor:
                    return new TL_channelFull_layer144();
                case TL_channelFull_layer139.constructor:
                    return new TL_channelFull_layer139();
                case TL_channelFull_layer135.constructor:
                    return new TL_channelFull_layer135();
                case TL_channelFull_layer134.constructor:
                    return new TL_channelFull_layer134();
                case TL_channelFull_layer98.constructor:
                    return new TL_channelFull_layer98();
                case TL_channelFull_layer99.constructor:
                    return new TL_channelFull_layer99();
                case TL_chatFull_layer87.constructor:
                    return new TL_chatFull_layer87();
                case TL_channelFull_layer122.constructor:
                    return new TL_channelFull_layer122();
                case TL_channelFull_layer121.constructor:
                    return new TL_channelFull_layer121();
                case TL_channelFull_layer110.constructor:
                    return new TL_channelFull_layer110();
                case TL_channelFull_layer103.constructor:
                    return new TL_channelFull_layer103();
                case TL_channelFull_layer101.constructor:
                    return new TL_channelFull_layer101();
                case TL_channelFull_layer71.constructor:
                    return new TL_channelFull_layer71();
                case TL_channelFull_layer72.constructor:
                    return new TL_channelFull_layer72();
                case TL_channelFull_layer52.constructor:
                    return new TL_channelFull_layer52();
                case TL_channelFull_layer67.constructor:
                    return new TL_channelFull_layer67();
                case TL_channelFull_layer48.constructor:
                    return new TL_channelFull_layer48();
                case TL_chatFull_layer122.constructor:
                    return new TL_chatFull_layer122();
                case TL_chatFull_layer123.constructor:
                    return new TL_chatFull_layer123();
                case TL_channelFull_layer123.constructor:
                    return new TL_channelFull_layer123();
                case TL_chatFull_layer124.constructor:
                    return new TL_chatFull_layer124();
                case TL_channelFull_layer124.constructor:
                    return new TL_channelFull_layer124();
                case TL_chatFull_layer131.constructor:
                    return new TL_chatFull_layer131();
                case TL_channelFull_layer131.constructor:
                    return new TL_channelFull_layer131();
                case TL_chatFull_layer132.constructor:
                    return new TL_chatFull_layer132();
                case TL_channelFull_layer132.constructor:
                    return new TL_channelFull_layer132();
                case TL_chatFull_layer133.constructor:
                    return new TL_chatFull_layer133();
                case TL_channelFull_layer133.constructor:
                    return new TL_channelFull_layer133();
                case TL_chatFull_layer121.constructor:
                    return new TL_chatFull_layer121();
                case TL_chatFull_layer98.constructor:
                    return new TL_chatFull_layer98();
                case TL_chatFull_layer92.constructor:
                    return new TL_chatFull_layer92();
                case TL_chatFull_layer135.constructor:
                    return new TL_chatFull_layer135();
                case TL_channelFull_old.constructor:
                    return new TL_channelFull_old();
                case TL_channelFull_layer70.constructor:
                    return new TL_channelFull_layer70();
                case TL_channelFull_layer89.constructor:
                    return new TL_channelFull_layer89();
                default:
                    return null;
            }
        }

        public static ChatFull TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            ChatFull result = TLdeserialize(ChatFull.class, fromConstructor(constructor), stream, constructor, exception);
            if (result != null && result.available_reactions == null) {
                if (!result.available_reactions_legacy.isEmpty()) {
                    TL_chatReactionsSome someReactions = new TL_chatReactionsSome();
                    for (int i = 0; i < result.available_reactions_legacy.size(); i++) {
                        TL_reactionEmoji reaction = new TL_reactionEmoji();
                        reaction.emoticon = result.available_reactions_legacy.get(i);
                        someReactions.reactions.add(reaction);
                    }
                    result.available_reactions = someReactions;
                } else {
                    result.available_reactions = new TL_chatReactionsNone();
                }
            }
            return result;
        }
    }

    public static class TL_channelFull_layer110 extends ChatFull {
        public static final int constructor = 0x2d895c74;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            can_view_participants = hasFlag(flags, FLAG_3);
            can_set_username = hasFlag(flags, FLAG_6);
            can_set_stickers = hasFlag(flags, FLAG_7);
            hidden_prehistory = hasFlag(flags, FLAG_10);
            can_view_stats = hasFlag(flags, FLAG_12);
            can_set_location = hasFlag(flags, FLAG_16);
            has_scheduled = hasFlag(flags, FLAG_19);
            id = stream.readInt32(exception);
            about = stream.readString(exception);
            if (hasFlag(flags, FLAG_0)) {
                participants_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_1)) {
                admins_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                kicked_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                banned_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_13)) {
                online_count = stream.readInt32(exception);
            }
            read_inbox_max_id = stream.readInt32(exception);
            read_outbox_max_id = stream.readInt32(exception);
            unread_count = stream.readInt32(exception);
            chat_photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
            notify_settings = PeerNotifySettings.TLdeserialize(stream, stream.readInt32(exception), exception);
            ExportedChatInvite invite = ExportedChatInvite.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (invite instanceof TL_chatInviteExported) {
                exported_invite = (TL_chatInviteExported) invite;
            }
            bot_info = Vector.deserialize(stream, TL_bots.BotInfo::TLdeserialize, exception);
            if (hasFlag(flags, FLAG_4)) {
                migrated_from_chat_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                migrated_from_max_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_5)) {
                pinned_msg_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_8)) {
                stickerset = StickerSet.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_9)) {
                available_min_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_11)) {
                folder_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_14)) {
                linked_chat_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_15)) {
                location = ChannelLocation.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_17)) {
                slowmode_seconds = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_18)) {
                slowmode_next_send_date = stream.readInt32(exception);
            }
            pts = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_3, can_view_participants);
            flags = setFlag(flags, FLAG_6, can_set_username);
            flags = setFlag(flags, FLAG_7, can_set_stickers);
            flags = setFlag(flags, FLAG_10, hidden_prehistory);
            flags = setFlag(flags, FLAG_12, can_view_stats);
            flags = setFlag(flags, FLAG_16, can_set_location);
            flags = setFlag(flags, FLAG_19, has_scheduled);
            stream.writeInt32(flags);
            stream.writeInt32((int) id);
            stream.writeString(about);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeInt32(participants_count);
            }
            if (hasFlag(flags, FLAG_1)) {
                stream.writeInt32(admins_count);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(kicked_count);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(banned_count);
            }
            if (hasFlag(flags, FLAG_13)) {
                stream.writeInt32(online_count);
            }
            stream.writeInt32(read_inbox_max_id);
            stream.writeInt32(read_outbox_max_id);
            stream.writeInt32(unread_count);
            chat_photo.serializeToStream(stream);
            notify_settings.serializeToStream(stream);
            exported_invite.serializeToStream(stream);
            Vector.serialize(stream, bot_info);
            if (hasFlag(flags, FLAG_4)) {
                stream.writeInt32((int) migrated_from_chat_id);
            }
            if (hasFlag(flags, FLAG_4)) {
                stream.writeInt32(migrated_from_max_id);
            }
            if (hasFlag(flags, FLAG_5)) {
                stream.writeInt32(pinned_msg_id);
            }
            if (hasFlag(flags, FLAG_8)) {
                stickerset.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_9)) {
                stream.writeInt32(available_min_id);
            }
            if (hasFlag(flags, FLAG_11)) {
                stream.writeInt32(folder_id);
            }
            if (hasFlag(flags, FLAG_14)) {
                stream.writeInt32((int) linked_chat_id);
            }
            if (hasFlag(flags, FLAG_15)) {
                location.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_17)) {
                stream.writeInt32(slowmode_seconds);
            }
            if (hasFlag(flags, FLAG_18)) {
                stream.writeInt32(slowmode_next_send_date);
            }
            stream.writeInt32(pts);
        }
    }

    public static class TL_chatFull_layer124 extends TL_chatFull {
        public static final int constructor = 0xf06c4018;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            can_set_username = hasFlag(flags, FLAG_7);
            has_scheduled = hasFlag(flags, FLAG_8);
            id = stream.readInt32(exception);
            about = stream.readString(exception);
            participants = ChatParticipants.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_2)) {
                chat_photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            notify_settings = PeerNotifySettings.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_13)) {
                exported_invite = (TL_chatInviteExported) ExportedChatInvite.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_3)) {
                bot_info = Vector.deserialize(stream, TL_bots.BotInfo::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_6)) {
                pinned_msg_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_11)) {
                folder_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_12)) {
                call = InputGroupCall.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_14)) {
                ttl_period = stream.readInt32(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_7, can_set_username);
            flags = setFlag(flags, FLAG_8, has_scheduled);
            stream.writeInt32(flags);
            stream.writeInt32((int) id);
            stream.writeString(about);
            participants.serializeToStream(stream);
            if (hasFlag(flags, FLAG_2)) {
                chat_photo.serializeToStream(stream);
            }
            notify_settings.serializeToStream(stream);
            if (hasFlag(flags, FLAG_13)) {
                exported_invite.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_3)) {
                Vector.serialize(stream, bot_info);
            }
            if (hasFlag(flags, FLAG_6)) {
                stream.writeInt32(pinned_msg_id);
            }
            if (hasFlag(flags, FLAG_11)) {
                stream.writeInt32(folder_id);
            }
            if (hasFlag(flags, FLAG_12)) {
                call.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_14)) {
                stream.writeInt32(ttl_period);
            }
        }
    }

    public static class TL_channelFull_layer124 extends TL_channelFull {
        public static final int constructor = 0x2548c037;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            can_view_participants = hasFlag(flags, FLAG_3);
            can_set_username = hasFlag(flags, FLAG_6);
            can_set_stickers = hasFlag(flags, FLAG_7);
            hidden_prehistory = hasFlag(flags, FLAG_10);
            can_set_location = hasFlag(flags, FLAG_16);
            has_scheduled = hasFlag(flags, FLAG_19);
            can_view_stats = hasFlag(flags, FLAG_20);
            blocked = hasFlag(flags, FLAG_22);
            id = stream.readInt32(exception);
            about = stream.readString(exception);
            if (hasFlag(flags, FLAG_0)) {
                participants_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_1)) {
                admins_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                kicked_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                banned_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_13)) {
                online_count = stream.readInt32(exception);
            }
            read_inbox_max_id = stream.readInt32(exception);
            read_outbox_max_id = stream.readInt32(exception);
            unread_count = stream.readInt32(exception);
            chat_photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
            notify_settings = PeerNotifySettings.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_23)) {
                exported_invite = (TL_chatInviteExported) ExportedChatInvite.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            bot_info = Vector.deserialize(stream, TL_bots.BotInfo::TLdeserialize, exception);
            if (hasFlag(flags, FLAG_4)) {
                migrated_from_chat_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                migrated_from_max_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_5)) {
                pinned_msg_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_8)) {
                stickerset = StickerSet.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_9)) {
                available_min_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_11)) {
                folder_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_14)) {
                linked_chat_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_15)) {
                location = ChannelLocation.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_17)) {
                slowmode_seconds = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_18)) {
                slowmode_next_send_date = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_12)) {
                stats_dc = stream.readInt32(exception);
            }
            pts = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_21)) {
                call = InputGroupCall.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_24)) {
                ttl_period = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_25)) {
                pending_suggestions = Vector.deserializeString(stream, exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_3, can_view_participants);
            flags = setFlag(flags, FLAG_6, can_set_username);
            flags = setFlag(flags, FLAG_7, can_set_stickers);
            flags = setFlag(flags, FLAG_10, hidden_prehistory);
            flags = setFlag(flags, FLAG_16, can_set_location);
            flags = setFlag(flags, FLAG_19, has_scheduled);
            flags = setFlag(flags, FLAG_20, can_view_stats);
            flags = setFlag(flags, FLAG_22, blocked);
            stream.writeInt32(flags);
            stream.writeInt32((int) id);
            stream.writeString(about);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeInt32(participants_count);
            }
            if (hasFlag(flags, FLAG_1)) {
                stream.writeInt32(admins_count);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(kicked_count);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(banned_count);
            }
            if (hasFlag(flags, FLAG_13)) {
                stream.writeInt32(online_count);
            }
            stream.writeInt32(read_inbox_max_id);
            stream.writeInt32(read_outbox_max_id);
            stream.writeInt32(unread_count);
            chat_photo.serializeToStream(stream);
            notify_settings.serializeToStream(stream);
            if (hasFlag(flags, FLAG_23)) {
                exported_invite.serializeToStream(stream);
            }
            Vector.serialize(stream, bot_info);
            if (hasFlag(flags, FLAG_4)) {
                stream.writeInt32((int) migrated_from_chat_id);
            }
            if (hasFlag(flags, FLAG_4)) {
                stream.writeInt32(migrated_from_max_id);
            }
            if (hasFlag(flags, FLAG_5)) {
                stream.writeInt32(pinned_msg_id);
            }
            if (hasFlag(flags, FLAG_8)) {
                stickerset.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_9)) {
                stream.writeInt32(available_min_id);
            }
            if (hasFlag(flags, FLAG_11)) {
                stream.writeInt32(folder_id);
            }
            if (hasFlag(flags, FLAG_14)) {
                stream.writeInt32((int) linked_chat_id);
            }
            if (hasFlag(flags, FLAG_15)) {
                location.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_17)) {
                stream.writeInt32(slowmode_seconds);
            }
            if (hasFlag(flags, FLAG_18)) {
                stream.writeInt32(slowmode_next_send_date);
            }
            if (hasFlag(flags, FLAG_12)) {
                stream.writeInt32(stats_dc);
            }
            stream.writeInt32(pts);
            if (hasFlag(flags, FLAG_21)) {
                call.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_24)) {
                stream.writeInt32(ttl_period);
            }
            if (hasFlag(flags, FLAG_25)) {
                Vector.serializeString(stream, pending_suggestions);
            }
        }
    }

    public static class TL_chatFull_layer123 extends TL_chatFull {
        public static final int constructor = 0xf3474af6;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            can_set_username = hasFlag(flags, FLAG_7);
            has_scheduled = hasFlag(flags, FLAG_8);
            id = stream.readInt32(exception);
            about = stream.readString(exception);
            participants = ChatParticipants.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_2)) {
                chat_photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            notify_settings = PeerNotifySettings.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_13)) {
                exported_invite = (TL_chatInviteExported) ExportedChatInvite.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_3)) {
                bot_info = Vector.deserialize(stream, TL_bots.BotInfo::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_6)) {
                pinned_msg_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_11)) {
                folder_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_12)) {
                call = InputGroupCall.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_7, can_set_username);
            flags = setFlag(flags, FLAG_8, has_scheduled);
            stream.writeInt32(flags);
            stream.writeInt32((int) id);
            stream.writeString(about);
            participants.serializeToStream(stream);
            if (hasFlag(flags, FLAG_2)) {
                chat_photo.serializeToStream(stream);
            }
            notify_settings.serializeToStream(stream);
            if (hasFlag(flags, FLAG_13)) {
                exported_invite.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_3)) {
                Vector.serialize(stream, bot_info);
            }
            if (hasFlag(flags, FLAG_6)) {
                stream.writeInt32(pinned_msg_id);
            }
            if (hasFlag(flags, FLAG_11)) {
                stream.writeInt32(folder_id);
            }
            if (hasFlag(flags, FLAG_12)) {
                call.serializeToStream(stream);
            }
        }
    }

    public static class TL_channelFull_layer123 extends TL_channelFull {
        public static final int constructor = 0x7a7de4f7;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            can_view_participants = hasFlag(flags, FLAG_3);
            can_set_username = hasFlag(flags, FLAG_6);
            can_set_stickers = hasFlag(flags, FLAG_7);
            hidden_prehistory = hasFlag(flags, FLAG_10);
            can_set_location = hasFlag(flags, FLAG_16);
            has_scheduled = hasFlag(flags, FLAG_19);
            can_view_stats = hasFlag(flags, FLAG_20);
            blocked = hasFlag(flags, FLAG_22);
            id = stream.readInt32(exception);
            about = stream.readString(exception);
            if (hasFlag(flags, FLAG_0)) {
                participants_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_1)) {
                admins_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                kicked_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                banned_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_13)) {
                online_count = stream.readInt32(exception);
            }
            read_inbox_max_id = stream.readInt32(exception);
            read_outbox_max_id = stream.readInt32(exception);
            unread_count = stream.readInt32(exception);
            chat_photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
            notify_settings = PeerNotifySettings.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_23)) {
                exported_invite = (TL_chatInviteExported) ExportedChatInvite.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            bot_info = Vector.deserialize(stream, TL_bots.BotInfo::TLdeserialize, exception);
            if (hasFlag(flags, FLAG_4)) {
                migrated_from_chat_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                migrated_from_max_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_5)) {
                pinned_msg_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_8)) {
                stickerset = StickerSet.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_9)) {
                available_min_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_11)) {
                folder_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_14)) {
                linked_chat_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_15)) {
                location = ChannelLocation.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_17)) {
                slowmode_seconds = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_18)) {
                slowmode_next_send_date = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_12)) {
                stats_dc = stream.readInt32(exception);
            }
            pts = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_21)) {
                call = InputGroupCall.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_3, can_view_participants);
            flags = setFlag(flags, FLAG_6, can_set_username);
            flags = setFlag(flags, FLAG_7, can_set_stickers);
            flags = setFlag(flags, FLAG_10, hidden_prehistory);
            flags = setFlag(flags, FLAG_16, can_set_location);
            flags = setFlag(flags, FLAG_19, has_scheduled);
            flags = setFlag(flags, FLAG_20, can_view_stats);
            flags = setFlag(flags, FLAG_22, blocked);
            stream.writeInt32(flags);
            stream.writeInt32((int) id);
            stream.writeString(about);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeInt32(participants_count);
            }
            if (hasFlag(flags, FLAG_1)) {
                stream.writeInt32(admins_count);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(kicked_count);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(banned_count);
            }
            if (hasFlag(flags, FLAG_13)) {
                stream.writeInt32(online_count);
            }
            stream.writeInt32(read_inbox_max_id);
            stream.writeInt32(read_outbox_max_id);
            stream.writeInt32(unread_count);
            chat_photo.serializeToStream(stream);
            notify_settings.serializeToStream(stream);
            if (hasFlag(flags, FLAG_23)) {
                exported_invite.serializeToStream(stream);
            }
            Vector.serialize(stream, bot_info);
            if (hasFlag(flags, FLAG_4)) {
                stream.writeInt32((int) migrated_from_chat_id);
            }
            if (hasFlag(flags, FLAG_4)) {
                stream.writeInt32(migrated_from_max_id);
            }
            if (hasFlag(flags, FLAG_5)) {
                stream.writeInt32(pinned_msg_id);
            }
            if (hasFlag(flags, FLAG_8)) {
                stickerset.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_9)) {
                stream.writeInt32(available_min_id);
            }
            if (hasFlag(flags, FLAG_11)) {
                stream.writeInt32(folder_id);
            }
            if (hasFlag(flags, FLAG_14)) {
                stream.writeInt32((int) linked_chat_id);
            }
            if (hasFlag(flags, FLAG_15)) {
                location.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_17)) {
                stream.writeInt32(slowmode_seconds);
            }
            if (hasFlag(flags, FLAG_18)) {
                stream.writeInt32(slowmode_next_send_date);
            }
            if (hasFlag(flags, FLAG_12)) {
                stream.writeInt32(stats_dc);
            }
            stream.writeInt32(pts);
            if (hasFlag(flags, FLAG_21)) {
                call.serializeToStream(stream);
            }
        }
    }

    public static class TL_chatFull_layer131 extends TL_chatFull {
        public static final int constructor = 0x8a1e2983;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            can_set_username = hasFlag(flags, FLAG_7);
            has_scheduled = hasFlag(flags, FLAG_8);
            id = stream.readInt32(exception);
            about = stream.readString(exception);
            participants = ChatParticipants.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_2)) {
                chat_photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            notify_settings = PeerNotifySettings.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_13)) {
                exported_invite = (TLRPC.TL_chatInviteExported) ExportedChatInvite.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_3)) {
                bot_info = Vector.deserialize(stream, TL_bots.BotInfo::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_6)) {
                pinned_msg_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_11)) {
                folder_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_12)) {
                call = InputGroupCall.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_14)) {
                ttl_period = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_15)) {
                groupcall_default_join_as = Peer.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_7, can_set_username);
            flags = setFlag(flags, FLAG_8, has_scheduled);
            stream.writeInt32(flags);
            stream.writeInt32((int) id);
            stream.writeString(about);
            participants.serializeToStream(stream);
            if (hasFlag(flags, FLAG_2)) {
                chat_photo.serializeToStream(stream);
            }
            notify_settings.serializeToStream(stream);
            if (hasFlag(flags, FLAG_13)) {
                exported_invite.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_3)) {
                Vector.serialize(stream, bot_info);
            }
            if (hasFlag(flags, FLAG_6)) {
                stream.writeInt32(pinned_msg_id);
            }
            if (hasFlag(flags, FLAG_11)) {
                stream.writeInt32(folder_id);
            }
            if (hasFlag(flags, FLAG_12)) {
                call.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_14)) {
                stream.writeInt32(ttl_period);
            }
            if (hasFlag(flags, FLAG_15)) {
                groupcall_default_join_as.serializeToStream(stream);
            }
        }
    }

    public static class TL_chatFull_layer132 extends TL_chatFull {
        public static final int constructor = 0x49a0a5d9;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            can_set_username = hasFlag(flags, FLAG_7);
            has_scheduled = hasFlag(flags, FLAG_8);
            id = stream.readInt32(exception);
            about = stream.readString(exception);
            participants = ChatParticipants.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_2)) {
                chat_photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            notify_settings = PeerNotifySettings.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_13)) {
                exported_invite = (TLRPC.TL_chatInviteExported) ExportedChatInvite.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_3)) {
                bot_info = Vector.deserialize(stream, TL_bots.BotInfo::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_6)) {
                pinned_msg_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_11)) {
                folder_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_12)) {
                call = InputGroupCall.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_14)) {
                ttl_period = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_15)) {
                groupcall_default_join_as = Peer.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_16)) {
                theme_emoticon = stream.readString(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_7, can_set_username);
            flags = setFlag(flags, FLAG_8, has_scheduled);
            stream.writeInt32(flags);
            stream.writeInt32((int) id);
            stream.writeString(about);
            participants.serializeToStream(stream);
            if (hasFlag(flags, FLAG_2)) {
                chat_photo.serializeToStream(stream);
            }
            notify_settings.serializeToStream(stream);
            if (hasFlag(flags, FLAG_13)) {
                exported_invite.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_3)) {
                Vector.serialize(stream, bot_info);
            }
            if (hasFlag(flags, FLAG_6)) {
                stream.writeInt32(pinned_msg_id);
            }
            if (hasFlag(flags, FLAG_11)) {
                stream.writeInt32(folder_id);
            }
            if (hasFlag(flags, FLAG_12)) {
                call.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_14)) {
                stream.writeInt32(ttl_period);
            }
            if (hasFlag(flags, FLAG_15)) {
                groupcall_default_join_as.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_16)) {
                stream.writeString(theme_emoticon);
            }
        }
    }

    public static class TL_channelFull_layer132 extends TL_channelFull {
        public static final int constructor = 0x2f532f3c;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            can_view_participants = hasFlag(flags, FLAG_3);
            can_set_username = hasFlag(flags, FLAG_6);
            can_set_stickers = hasFlag(flags, FLAG_7);
            hidden_prehistory = hasFlag(flags, FLAG_10);
            can_set_location = hasFlag(flags, FLAG_16);
            has_scheduled = hasFlag(flags, FLAG_19);
            can_view_stats = hasFlag(flags, FLAG_20);
            blocked = hasFlag(flags, FLAG_22);
            id = stream.readInt32(exception);
            about = stream.readString(exception);
            if (hasFlag(flags, FLAG_0)) {
                participants_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_1)) {
                admins_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                kicked_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                banned_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_13)) {
                online_count = stream.readInt32(exception);
            }
            read_inbox_max_id = stream.readInt32(exception);
            read_outbox_max_id = stream.readInt32(exception);
            unread_count = stream.readInt32(exception);
            chat_photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
            notify_settings = PeerNotifySettings.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_23)) {
                exported_invite = (TLRPC.TL_chatInviteExported) ExportedChatInvite.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            bot_info = Vector.deserialize(stream, TL_bots.BotInfo::TLdeserialize, exception);
            if (hasFlag(flags, FLAG_4)) {
                migrated_from_chat_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                migrated_from_max_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_5)) {
                pinned_msg_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_8)) {
                stickerset = StickerSet.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_9)) {
                available_min_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_11)) {
                folder_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_14)) {
                linked_chat_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_15)) {
                location = ChannelLocation.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_17)) {
                slowmode_seconds = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_18)) {
                slowmode_next_send_date = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_12)) {
                stats_dc = stream.readInt32(exception);
            }
            pts = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_21)) {
                call = InputGroupCall.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_24)) {
                ttl_period = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_25)) {
                pending_suggestions = Vector.deserializeString(stream, exception);
            }
            if (hasFlag(flags, FLAG_26)) {
                groupcall_default_join_as = Peer.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_27)) {
                theme_emoticon = stream.readString(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_3, can_view_participants);
            flags = setFlag(flags, FLAG_6, can_set_username);
            flags = setFlag(flags, FLAG_7, can_set_stickers);
            flags = setFlag(flags, FLAG_10, hidden_prehistory);
            flags = setFlag(flags, FLAG_16, can_set_location);
            flags = setFlag(flags, FLAG_19, has_scheduled);
            flags = setFlag(flags, FLAG_20, can_view_stats);
            flags = setFlag(flags, FLAG_22, blocked);
            stream.writeInt32(flags);
            stream.writeInt32((int) id);
            stream.writeString(about);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeInt32(participants_count);
            }
            if (hasFlag(flags, FLAG_1)) {
                stream.writeInt32(admins_count);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(kicked_count);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(banned_count);
            }
            if (hasFlag(flags, FLAG_13)) {
                stream.writeInt32(online_count);
            }
            stream.writeInt32(read_inbox_max_id);
            stream.writeInt32(read_outbox_max_id);
            stream.writeInt32(unread_count);
            chat_photo.serializeToStream(stream);
            notify_settings.serializeToStream(stream);
            if (hasFlag(flags, FLAG_23)) {
                exported_invite.serializeToStream(stream);
            }
            Vector.serialize(stream, bot_info);
            if (hasFlag(flags, FLAG_4)) {
                stream.writeInt32((int) migrated_from_chat_id);
            }
            if (hasFlag(flags, FLAG_4)) {
                stream.writeInt32(migrated_from_max_id);
            }
            if (hasFlag(flags, FLAG_5)) {
                stream.writeInt32(pinned_msg_id);
            }
            if (hasFlag(flags, FLAG_8)) {
                stickerset.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_9)) {
                stream.writeInt32(available_min_id);
            }
            if (hasFlag(flags, FLAG_11)) {
                stream.writeInt32(folder_id);
            }
            if (hasFlag(flags, FLAG_14)) {
                stream.writeInt32((int) linked_chat_id);
            }
            if (hasFlag(flags, FLAG_15)) {
                location.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_17)) {
                stream.writeInt32(slowmode_seconds);
            }
            if (hasFlag(flags, FLAG_18)) {
                stream.writeInt32(slowmode_next_send_date);
            }
            if (hasFlag(flags, FLAG_12)) {
                stream.writeInt32(stats_dc);
            }
            stream.writeInt32(pts);
            if (hasFlag(flags, FLAG_21)) {
                call.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_24)) {
                stream.writeInt32(ttl_period);
            }
            if (hasFlag(flags, FLAG_25)) {
                Vector.serializeString(stream, pending_suggestions);
            }
            if (hasFlag(flags, FLAG_26)) {
                groupcall_default_join_as.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_27)) {
                stream.writeString(theme_emoticon);
            }
        }
    }

    public static class TL_chatFull_layer133 extends ChatFull {
        public static final int constructor = 0x4dbdc099;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            can_set_username = hasFlag(flags, FLAG_7);
            has_scheduled = hasFlag(flags, FLAG_8);
            id = stream.readInt64(exception);
            about = stream.readString(exception);
            participants = ChatParticipants.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_2)) {
                chat_photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            notify_settings = PeerNotifySettings.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_13)) {
                exported_invite = (TLRPC.TL_chatInviteExported) ExportedChatInvite.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_3)) {
                bot_info = Vector.deserialize(stream, TL_bots.BotInfo::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_6)) {
                pinned_msg_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_11)) {
                folder_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_12)) {
                call = InputGroupCall.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_14)) {
                ttl_period = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_15)) {
                groupcall_default_join_as = Peer.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_16)) {
                theme_emoticon = stream.readString(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_7, can_set_username);
            flags = setFlag(flags, FLAG_8, has_scheduled);
            stream.writeInt32(flags);
            stream.writeInt64(id);
            stream.writeString(about);
            participants.serializeToStream(stream);
            if (hasFlag(flags, FLAG_2)) {
                chat_photo.serializeToStream(stream);
            }
            notify_settings.serializeToStream(stream);
            if (hasFlag(flags, FLAG_13)) {
                exported_invite.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_3)) {
                Vector.serialize(stream, bot_info);
            }
            if (hasFlag(flags, FLAG_6)) {
                stream.writeInt32(pinned_msg_id);
            }
            if (hasFlag(flags, FLAG_11)) {
                stream.writeInt32(folder_id);
            }
            if (hasFlag(flags, FLAG_12)) {
                call.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_14)) {
                stream.writeInt32(ttl_period);
            }
            if (hasFlag(flags, FLAG_15)) {
                groupcall_default_join_as.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_16)) {
                stream.writeString(theme_emoticon);
            }
        }
    }

    public static class TL_chatFull_layer135 extends ChatFull {
        public static final int constructor = 0x46a6ffb4;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            can_set_username = hasFlag(flags, FLAG_7);
            has_scheduled = hasFlag(flags, FLAG_8);
            id = stream.readInt64(exception);
            about = stream.readString(exception);
            participants = ChatParticipants.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_2)) {
                chat_photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            notify_settings = PeerNotifySettings.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_13)) {
                exported_invite = ExportedChatInvite.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_3)) {
                bot_info = Vector.deserialize(stream, TL_bots.BotInfo::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_6)) {
                pinned_msg_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_11)) {
                folder_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_12)) {
                call = InputGroupCall.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_14)) {
                ttl_period = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_15)) {
                groupcall_default_join_as = Peer.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_16)) {
                theme_emoticon = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_17)) {
                requests_pending = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_17)) {
                recent_requesters = Vector.deserializeLong(stream, exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_7, can_set_username);
            flags = setFlag(flags, FLAG_8, has_scheduled);
            stream.writeInt32(flags);
            stream.writeInt64(id);
            stream.writeString(about);
            participants.serializeToStream(stream);
            if (hasFlag(flags, FLAG_2)) {
                chat_photo.serializeToStream(stream);
            }
            notify_settings.serializeToStream(stream);
            if (hasFlag(flags, FLAG_13)) {
                exported_invite.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_3)) {
                Vector.serialize(stream, bot_info);
            }
            if (hasFlag(flags, FLAG_6)) {
                stream.writeInt32(pinned_msg_id);
            }
            if (hasFlag(flags, FLAG_11)) {
                stream.writeInt32(folder_id);
            }
            if (hasFlag(flags, FLAG_12)) {
                call.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_14)) {
                stream.writeInt32(ttl_period);
            }
            if (hasFlag(flags, FLAG_15)) {
                groupcall_default_join_as.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_16)) {
                stream.writeString(theme_emoticon);
            }
            if (hasFlag(flags, FLAG_17)) {
                stream.writeInt32(requests_pending);
            }
            if (hasFlag(flags, FLAG_17)) {
                Vector.serializeLong(stream, recent_requesters);
            }
        }
    }

    public static class TL_channelFull_layer135 extends ChatFull {
        public static final int constructor = 0x56662e2e;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            can_view_participants = hasFlag(flags, FLAG_3);
            can_set_username = hasFlag(flags, FLAG_6);
            can_set_stickers = hasFlag(flags, FLAG_7);
            hidden_prehistory = hasFlag(flags, FLAG_10);
            can_set_location = hasFlag(flags, FLAG_16);
            has_scheduled = hasFlag(flags, FLAG_19);
            can_view_stats = hasFlag(flags, FLAG_20);
            blocked = hasFlag(flags, FLAG_22);
            id = stream.readInt64(exception);
            about = stream.readString(exception);
            if (hasFlag(flags, FLAG_0)) {
                participants_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_1)) {
                admins_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                kicked_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                banned_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_13)) {
                online_count = stream.readInt32(exception);
            }
            read_inbox_max_id = stream.readInt32(exception);
            read_outbox_max_id = stream.readInt32(exception);
            unread_count = stream.readInt32(exception);
            chat_photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
            notify_settings = PeerNotifySettings.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_23)) {
                exported_invite = ExportedChatInvite.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            bot_info = Vector.deserialize(stream, TL_bots.BotInfo::TLdeserialize, exception);
            if (hasFlag(flags, FLAG_4)) {
                migrated_from_chat_id = stream.readInt64(exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                migrated_from_max_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_5)) {
                pinned_msg_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_8)) {
                stickerset = StickerSet.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_9)) {
                available_min_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_11)) {
                folder_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_14)) {
                linked_chat_id = stream.readInt64(exception);
            }
            if (hasFlag(flags, FLAG_15)) {
                location = ChannelLocation.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_17)) {
                slowmode_seconds = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_18)) {
                slowmode_next_send_date = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_12)) {
                stats_dc = stream.readInt32(exception);
            }
            pts = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_21)) {
                call = InputGroupCall.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_24)) {
                ttl_period = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_25)) {
                pending_suggestions = Vector.deserializeString(stream, exception);
            }
            if (hasFlag(flags, FLAG_26)) {
                groupcall_default_join_as = Peer.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_27)) {
                theme_emoticon = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_28)) {
                requests_pending = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_28)) {
                recent_requesters = Vector.deserializeLong(stream, exception);
            }
            if (hasFlag(flags, FLAG_29)) {
                default_send_as = Peer.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_3, can_view_participants);
            flags = setFlag(flags, FLAG_6, can_set_username);
            flags = setFlag(flags, FLAG_7, can_set_stickers);
            flags = setFlag(flags, FLAG_10, hidden_prehistory);
            flags = setFlag(flags, FLAG_16, can_set_location);
            flags = setFlag(flags, FLAG_19, has_scheduled);
            flags = setFlag(flags, FLAG_20, can_view_stats);
            flags = setFlag(flags, FLAG_22, blocked);
            stream.writeInt32(flags);
            stream.writeInt64(id);
            stream.writeString(about);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeInt32(participants_count);
            }
            if (hasFlag(flags, FLAG_1)) {
                stream.writeInt32(admins_count);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(kicked_count);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(banned_count);
            }
            if (hasFlag(flags, FLAG_13)) {
                stream.writeInt32(online_count);
            }
            stream.writeInt32(read_inbox_max_id);
            stream.writeInt32(read_outbox_max_id);
            stream.writeInt32(unread_count);
            chat_photo.serializeToStream(stream);
            notify_settings.serializeToStream(stream);
            if (hasFlag(flags, FLAG_23)) {
                exported_invite.serializeToStream(stream);
            }
            Vector.serialize(stream, bot_info);
            if (hasFlag(flags, FLAG_4)) {
                stream.writeInt64(migrated_from_chat_id);
            }
            if (hasFlag(flags, FLAG_4)) {
                stream.writeInt32(migrated_from_max_id);
            }
            if (hasFlag(flags, FLAG_5)) {
                stream.writeInt32(pinned_msg_id);
            }
            if (hasFlag(flags, FLAG_8)) {
                stickerset.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_9)) {
                stream.writeInt32(available_min_id);
            }
            if (hasFlag(flags, FLAG_11)) {
                stream.writeInt32(folder_id);
            }
            if (hasFlag(flags, FLAG_14)) {
                stream.writeInt64(linked_chat_id);
            }
            if (hasFlag(flags, FLAG_15)) {
                location.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_17)) {
                stream.writeInt32(slowmode_seconds);
            }
            if (hasFlag(flags, FLAG_18)) {
                stream.writeInt32(slowmode_next_send_date);
            }
            if (hasFlag(flags, FLAG_12)) {
                stream.writeInt32(stats_dc);
            }
            stream.writeInt32(pts);
            if (hasFlag(flags, FLAG_21)) {
                call.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_24)) {
                stream.writeInt32(ttl_period);
            }
            if (hasFlag(flags, FLAG_25)) {
                Vector.serializeString(stream, pending_suggestions);
            }
            if (hasFlag(flags, FLAG_26)) {
                groupcall_default_join_as.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_27)) {
                stream.writeString(theme_emoticon);
            }
            if (hasFlag(flags, FLAG_28)) {
                stream.writeInt32(requests_pending);
            }
            if (hasFlag(flags, FLAG_28)) {
                Vector.serializeLong(stream, recent_requesters);
            }
            if (hasFlag(flags, FLAG_29)) {
                default_send_as.serializeToStream(stream);
            }
        }
    }

    public static class TL_channelFull_layer134 extends ChatFull {
        public static final int constructor = 0x59cff963;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            can_view_participants = hasFlag(flags, FLAG_3);
            can_set_username = hasFlag(flags, FLAG_6);
            can_set_stickers = hasFlag(flags, FLAG_7);
            hidden_prehistory = hasFlag(flags, FLAG_10);
            can_set_location = hasFlag(flags, FLAG_16);
            has_scheduled = hasFlag(flags, FLAG_19);
            can_view_stats = hasFlag(flags, FLAG_20);
            blocked = hasFlag(flags, FLAG_22);
            id = stream.readInt64(exception);
            about = stream.readString(exception);
            if (hasFlag(flags, FLAG_0)) {
                participants_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_1)) {
                admins_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                kicked_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                banned_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_13)) {
                online_count = stream.readInt32(exception);
            }
            read_inbox_max_id = stream.readInt32(exception);
            read_outbox_max_id = stream.readInt32(exception);
            unread_count = stream.readInt32(exception);
            chat_photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
            notify_settings = PeerNotifySettings.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_23)) {
                exported_invite = ExportedChatInvite.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            bot_info = Vector.deserialize(stream, TL_bots.BotInfo::TLdeserialize, exception);
            if (hasFlag(flags, FLAG_4)) {
                migrated_from_chat_id = stream.readInt64(exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                migrated_from_max_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_5)) {
                pinned_msg_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_8)) {
                stickerset = StickerSet.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_9)) {
                available_min_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_11)) {
                folder_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_14)) {
                linked_chat_id = stream.readInt64(exception);
            }
            if (hasFlag(flags, FLAG_15)) {
                location = ChannelLocation.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_17)) {
                slowmode_seconds = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_18)) {
                slowmode_next_send_date = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_12)) {
                stats_dc = stream.readInt32(exception);
            }
            pts = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_21)) {
                call = InputGroupCall.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_24)) {
                ttl_period = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_25)) {
                pending_suggestions = Vector.deserializeString(stream, exception);
            }
            if (hasFlag(flags, FLAG_26)) {
                groupcall_default_join_as = Peer.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_27)) {
                theme_emoticon = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_28)) {
                requests_pending = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_28)) {
                recent_requesters = Vector.deserializeLong(stream, exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_3, can_view_participants);
            flags = setFlag(flags, FLAG_6, can_set_username);
            flags = setFlag(flags, FLAG_7, can_set_stickers);
            flags = setFlag(flags, FLAG_10, hidden_prehistory);
            flags = setFlag(flags, FLAG_16, can_set_location);
            flags = setFlag(flags, FLAG_19, has_scheduled);
            flags = setFlag(flags, FLAG_20, can_view_stats);
            flags = setFlag(flags, FLAG_22, blocked);
            stream.writeInt32(flags);
            stream.writeInt64(id);
            stream.writeString(about);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeInt32(participants_count);
            }
            if (hasFlag(flags, FLAG_1)) {
                stream.writeInt32(admins_count);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(kicked_count);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(banned_count);
            }
            if (hasFlag(flags, FLAG_13)) {
                stream.writeInt32(online_count);
            }
            stream.writeInt32(read_inbox_max_id);
            stream.writeInt32(read_outbox_max_id);
            stream.writeInt32(unread_count);
            chat_photo.serializeToStream(stream);
            notify_settings.serializeToStream(stream);
            if (hasFlag(flags, FLAG_23)) {
                exported_invite.serializeToStream(stream);
            }
            Vector.serialize(stream, bot_info);
            if (hasFlag(flags, FLAG_4)) {
                stream.writeInt64(migrated_from_chat_id);
            }
            if (hasFlag(flags, FLAG_4)) {
                stream.writeInt32(migrated_from_max_id);
            }
            if (hasFlag(flags, FLAG_5)) {
                stream.writeInt32(pinned_msg_id);
            }
            if (hasFlag(flags, FLAG_8)) {
                stickerset.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_9)) {
                stream.writeInt32(available_min_id);
            }
            if (hasFlag(flags, FLAG_11)) {
                stream.writeInt32(folder_id);
            }
            if (hasFlag(flags, FLAG_14)) {
                stream.writeInt64(linked_chat_id);
            }
            if (hasFlag(flags, FLAG_15)) {
                location.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_17)) {
                stream.writeInt32(slowmode_seconds);
            }
            if (hasFlag(flags, FLAG_18)) {
                stream.writeInt32(slowmode_next_send_date);
            }
            if (hasFlag(flags, FLAG_12)) {
                stream.writeInt32(stats_dc);
            }
            stream.writeInt32(pts);
            if (hasFlag(flags, FLAG_21)) {
                call.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_24)) {
                stream.writeInt32(ttl_period);
            }
            if (hasFlag(flags, FLAG_25)) {
                Vector.serializeString(stream, pending_suggestions);
            }
            if (hasFlag(flags, FLAG_26)) {
                groupcall_default_join_as.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_27)) {
                stream.writeString(theme_emoticon);
            }
            if (hasFlag(flags, FLAG_28)) {
                stream.writeInt32(requests_pending);
            }
            if (hasFlag(flags, FLAG_28)) {
                Vector.serializeLong(stream, recent_requesters);
            }
        }
    }

    public static class TL_channelFull_layer133 extends ChatFull {
        public static final int constructor = 0xe9b27a17;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            can_view_participants = hasFlag(flags, FLAG_3);
            can_set_username = hasFlag(flags, FLAG_6);
            can_set_stickers = hasFlag(flags, FLAG_7);
            hidden_prehistory = hasFlag(flags, FLAG_10);
            can_set_location = hasFlag(flags, FLAG_16);
            has_scheduled = hasFlag(flags, FLAG_19);
            can_view_stats = hasFlag(flags, FLAG_20);
            blocked = hasFlag(flags, FLAG_22);
            id = stream.readInt64(exception);
            about = stream.readString(exception);
            if (hasFlag(flags, FLAG_0)) {
                participants_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_1)) {
                admins_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                kicked_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                banned_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_13)) {
                online_count = stream.readInt32(exception);
            }
            read_inbox_max_id = stream.readInt32(exception);
            read_outbox_max_id = stream.readInt32(exception);
            unread_count = stream.readInt32(exception);
            chat_photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
            notify_settings = PeerNotifySettings.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_23)) {
                exported_invite = (TLRPC.TL_chatInviteExported) ExportedChatInvite.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            bot_info = Vector.deserialize(stream, TL_bots.BotInfo::TLdeserialize, exception);
            if (hasFlag(flags, FLAG_4)) {
                migrated_from_chat_id = stream.readInt64(exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                migrated_from_max_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_5)) {
                pinned_msg_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_8)) {
                stickerset = StickerSet.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_9)) {
                available_min_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_11)) {
                folder_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_14)) {
                linked_chat_id = stream.readInt64(exception);
            }
            if (hasFlag(flags, FLAG_15)) {
                location = ChannelLocation.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_17)) {
                slowmode_seconds = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_18)) {
                slowmode_next_send_date = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_12)) {
                stats_dc = stream.readInt32(exception);
            }
            pts = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_21)) {
                call = InputGroupCall.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_24)) {
                ttl_period = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_25)) {
                pending_suggestions = Vector.deserializeString(stream, exception);
            }
            if (hasFlag(flags, FLAG_26)) {
                groupcall_default_join_as = Peer.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_27)) {
                theme_emoticon = stream.readString(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_3, can_view_participants);
            flags = setFlag(flags, FLAG_6, can_set_username);
            flags = setFlag(flags, FLAG_7, can_set_stickers);
            flags = setFlag(flags, FLAG_10, hidden_prehistory);
            flags = setFlag(flags, FLAG_16, can_set_location);
            flags = setFlag(flags, FLAG_19, has_scheduled);
            flags = setFlag(flags, FLAG_20, can_view_stats);
            flags = setFlag(flags, FLAG_22, blocked);
            stream.writeInt32(flags);
            stream.writeInt64(id);
            stream.writeString(about);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeInt32(participants_count);
            }
            if (hasFlag(flags, FLAG_1)) {
                stream.writeInt32(admins_count);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(kicked_count);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(banned_count);
            }
            if (hasFlag(flags, FLAG_13)) {
                stream.writeInt32(online_count);
            }
            stream.writeInt32(read_inbox_max_id);
            stream.writeInt32(read_outbox_max_id);
            stream.writeInt32(unread_count);
            chat_photo.serializeToStream(stream);
            notify_settings.serializeToStream(stream);
            if (hasFlag(flags, FLAG_23)) {
                exported_invite.serializeToStream(stream);
            }
            Vector.serialize(stream, bot_info);
            if (hasFlag(flags, FLAG_4)) {
                stream.writeInt64(migrated_from_chat_id);
            }
            if (hasFlag(flags, FLAG_4)) {
                stream.writeInt32(migrated_from_max_id);
            }
            if (hasFlag(flags, FLAG_5)) {
                stream.writeInt32(pinned_msg_id);
            }
            if (hasFlag(flags, FLAG_8)) {
                stickerset.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_9)) {
                stream.writeInt32(available_min_id);
            }
            if (hasFlag(flags, FLAG_11)) {
                stream.writeInt32(folder_id);
            }
            if (hasFlag(flags, FLAG_14)) {
                stream.writeInt64(linked_chat_id);
            }
            if (hasFlag(flags, FLAG_15)) {
                location.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_17)) {
                stream.writeInt32(slowmode_seconds);
            }
            if (hasFlag(flags, FLAG_18)) {
                stream.writeInt32(slowmode_next_send_date);
            }
            if (hasFlag(flags, FLAG_12)) {
                stream.writeInt32(stats_dc);
            }
            stream.writeInt32(pts);
            if (hasFlag(flags, FLAG_21)) {
                call.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_24)) {
                stream.writeInt32(ttl_period);
            }
            if (hasFlag(flags, FLAG_25)) {
                Vector.serializeString(stream, pending_suggestions);
            }
            if (hasFlag(flags, FLAG_26)) {
                groupcall_default_join_as.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_27)) {
                stream.writeString(theme_emoticon);
            }
        }
    }

    public static class TL_chatFull extends ChatFull {
        public static final int constructor = 0x2633421b;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            can_set_username = hasFlag(flags, FLAG_7);
            has_scheduled = hasFlag(flags, FLAG_8);
            has_welcome_messages = hasFlag(flags, FLAG_21);
            id = stream.readInt64(exception);
            about = stream.readString(exception);
            participants = ChatParticipants.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_2)) {
                chat_photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            notify_settings = PeerNotifySettings.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_13)) {
                exported_invite = ExportedChatInvite.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_3)) {
                bot_info = Vector.deserialize(stream, TL_bots.BotInfo::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_6)) {
                pinned_msg_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_11)) {
                folder_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_12)) {
                call = InputGroupCall.TLdeserialize(stream, stream.rxxœ\˜cp(İfãäÄ¶mœØ¶mÛ¶mÛ¶m;9±mÛNæÃ½3·¦VU×zışëîİµ;ë2[Ûqƒ5äæƒªÉõ:j$™|¼!óL8Ñ†#{üÒd9ıš‘ FØBœ¯sá§'Ohñ×­dÜ%S,-ö‚, <Û‰»1E×Î€Ó¡ÖøŒc»ÄùgÏçç÷°kwàJ®(ï[äö’g|ìĞà’€·ø{÷Š­Ï÷NLïOÏKoa«I-ÌfrÔ×ØÅ×¢ä¼ZÄQó»ÂïñC!Ë{‚˜åÊv •D¨zX…—Ø18íxr¸m	İ^Ïƒq¡7b ŸŞ{‹¥Ş‰öÄc‘Ö›#…Œâo.ÈØè¯Û½EØ‹«‘{^Ëô¢pÌ´YE•iúß¤ƒ%66º#kaŸ´'æÈºxQc™b[¯6J2*¡¯äŸ|€Êµ.ÏÓÉj·#u;séxÅ³üógb7ûtßÍ[ëß:¿Z+Ÿ¨oOºcão_^h;v´Ïs/œšen\{_|ÊÇ`P³'·^‘?Ê7ßóIëß8°fÈ2?şh¥šŸõ”~mkÕ¼Ó¹VcC+´˜yoÜ”O;%¾yUŸ5›qt¡áõ÷~-B­ğÏîNÛ]ô‘aü!r./–B¼ÃO ™Ènn{ê´Qõ¹İI¨E0@­^ØP!Tq=¬J#]9ÒŸ}n/ÏNu|jj«|‘Çdîş¾3„¯ ÕõáVfµoHŒ_–|sG.Ôâı,ó¨Ê¬$ÍôH|PÑá4‘¯ØTÊ¶:s»oÒxÓ/³qMçj+n_°›Âfaj’ÉB/I^o½ŠI  §Ø™1,sˆ7º×^–ójgÓ¹·²¹9¢¥Ád©FÉm,¬ËG6Ïb÷ªãL‘ã[éíçşîıêx#‹HP*Ã˜Í'÷¸Ùn[øÖ°ö±ÁïscË®fTtyÌ×ÁÑ5Ù%<}Ó_çZÌaåê}Ü;}æHk%D<<oEö4Š4ECÀR†]·—.,4˜Í_?1Ğ0sA¶F_ßDe­¾ß½º"eùp¯\èÛ³Éo°…ˆ¬·ZÁ'àô´n^
ö#ä½ü2|ØøĞN²÷Ï1C'ùÎù!ÅªÙiÙ†ÃñĞuxvkq•¤:škïóù¼ûPä^¸1İ›Â˜®~_5Å;Ëê/`‡Va¡Ú¯U¹£ŒŒ¤ÍmÃ/T-‚G±v.‡IÀ$f³Pk|N&cæ8Ä9ğÎÙS~!díJ··ûg„?Î—àO,B™HŒaq™`d†IÒîïÿ@§z_ovØâğë-RK€JóqŒDIƒ,Õ®^K`ÎA‹É¶ ”µàÈçı,U¶_@aoi2u!—,ğÌQ’2ë£ÈwØy7o¨+¡„üúö²KQ—t¥Öå 3x?.`¤¼2ö¦«¸Öä€K#G~ƒ2óå+w†ãPŠEZ©>šï»¼pI<úJÃ°Êñ9‹ÉpÒÛ-”Ff¡!0ÜÒŞ&§N’åo%}]³xX¿Ú¶Ñİ&KøeÙó\jî¦À¶
^©ë„Æz‰9ƒAb³BÃÆFG¥ZÏÒRµ.ôUp`7~Fd¸ÔU„ËxÜÔWúÄB5»0šéD+œQL!'§uèZ¢¿Ô‘š5ûGh5<ZPynÕĞüã·şpøÚVLz¹ù]Ë¼Z8~K|Î	™ú¥ê9Ò(=F#E~‹/+oÛU ¨â‡xş—É,ª|—Nã°N®¾…vU½†«oNv\b²oÄT\¤vK.cı'ù`óÜ‘~ı ,™fÖ"İö½Äëy·nÛ¹Šw…{•å;zÊÊ³ËÙŞ—Ï-šybË°)õ‹'§ÀÓ•¤l¦L˜¡•V×ƒ£èÒKaé•Tş^+"Ğ…õô	7»êÎ¹£®|¯{ÅÜåWŞtÁÌµ‚—úïu&•ÄVÍç6ów°iÏ/ZcõöãM3fç`ò¶G<ƒø§!Â˜,d!ÖÊå-Y]¤ƒ«Æwê“tË‰w®MZøÔµBŞ/~nØT:Ãí%ä…Å˜UŠæ‹RŸMŞ ÖÃe8‘Ú–Ò@Ö7Ûœ¼$ë±ğÊW'wšís³«fƒœ¤¢m,Ø#Sg“äüƒF6İÕ,"mÛí­]JëTk÷ĞÃôY‚é£3øÑº ÛDY›¹“ˆì¾>¹Şy4ïµó·xÊ˜ï;ÎòıÓÜë‡DJ‘A4Reì¯ãL´Ìà«_'Ãb¾=Ş:xıûJ¹ëX¹ı
æ«J;E=IÜ(Ù'¼Z¦ƒn¹¾¾o¸ó¼G¹‹MˆÜ*5ßšŞ?,ß¿ºxÉ'T€ê5@{šÚuÙ’÷¬mX8lÃLë¸yµ-º¿}êvÊcŞÎDÆvBK;Jßï?ú¸‚gJikÍ9•Fõ3A?Ù;Æ!5¹„.¹gE+$ó"•ş4pÇ úàï„¯!EEtŞ×¡ÖŸØ4`ãÈªÈjn£½0m–ÕÒ5·M‰ï>·O’5™qc?N‚ö°\Áeíd›l0õ^|¿éMçé–½7›W‘Zë|^€Ğp© ÁÑõÔ=1÷ª¨¢?¿€ö©\-öfèUJ–aTÓ@´tŒÙRC·tÄdè¼éwnc?7'¸ Ù¿GèwBE}dÌëvJJ½‡ìvNn>F'¹ u\\PF\•\æÙVP·tÆ¼›ô¢¨x)ä½(òPÕ©µ„2gGœ$Y&£NÆë2î,.ÎêSÌ›4×[Õ}«™‹ƒë£DÄ4/´h™WY;¨épaô4Vå™©l^ˆT·.	5Qy%§BZãL”Ws5tìg¿g˜(s¨ŸÊ«*5RÉÍ’!0Mêæ[˜4Ò—¬yÈ,Ô1eÇ>Õ;&õQWŠg@rEÑT¢P76d¤l5(›éŒY´çVIœ”Ã‚RNævº$å÷)eÎ-æÒ*(4l>ÉNw3ã¤ä‹8éo÷¯ƒ:©MYğ¥gGCq¦\.#›7İ™ÊV7©¨§×{™2£L.gğ`!e¢ VfŒ«O¶5R†ZWFLoíQ$0&SU©û¼ö{HSÛª	Wƒ&®Y¦öÖs¡†ê·»ÂZ¢ …]­Lñ>Q–\›³O…d‰Çéoº¥Ç1˜
Le3¥Óš
Ä'í`D#‹“'Èç‹5¨1ğÇ+%w	ÄşVĞü·ş7"ş=K÷÷l‡Ûíaq?JõX>Œ –ÒB¦Y`İC´E ôK®÷˜GÜ“Ò{X¸º,’áßaCxÙğG]ö…Gd9ğ¯®!‡RsÌSMxÙQ\]–µzjvÍÒÒWşô®“Êl¡ŞñD™•ÜFn3¯mnã¿êî•|ÿü·¢›BE`UÛÿ·ç·Ü†‰À²‘ßtª¤Èo£X¶_˜çDfİ@‹ÀBfõ€‹kRj™(—”¶%4gïù³BaÎÀ•A® W”'Æ¯pFŒL!•ÿw0E˜sE¦Øú@†ü»'W¨°Ê3×{¢+;#}B²şTO˜œºÚ=&!„Âã‚w7;ş¯rõH”Ûı­Q ñ¡ËlÿíñïùıÇ€ãC™Ã¸:’Íáİ‘¯ïÍÎpc¯ âGİñëËÌ¢‡€ÂA#3¶…ì½0ˆQcmÛEüÿ)¡ˆQÅ¨ãÔşšb8åDŒ¼öúïŠrKœS…¦Áœø}WæúG#ªkä°™g»ğß>à0pøUÖ[2\ì=§ ³ß9À`°8ªâqlEZ¬N‡™%ü¿õ¿‘ï¯¹İ»às€W¼ÿĞûˆúÜÇıŞÅ± / /ÒQ€›À›+ m¬ï°v;İ±€×ÿRi¸‰çµ‚éŸ»*¸kÊÈ€mËïö/WY€şàş­P1~}ÿÃFÆ-şÿå
¤§½öß´{¸{(šbçğ÷#© d¼>ş‰š¬ 2°ÿÃ6i2ŒÌv?ûxKï¿.aÁ‹=	W“ìü—fÇã=V;gÓ¾¥ÿÅšHø?ØşjÃl{¯ô¯ÔÏÒÏš‰+µù-`äƒ(ª™T6	¡T!TËŒZÿ`-ßfPxc7ü×#Tƒ^óJŒ‹:6ßÊxæiûØİó§daû¯mş…NôhêØŒ	êØX&œ¥ş—.Y~&şÿ¹ÁŒÕ,à¦ëL‘0¦¦g¦lŠÿ°€YhW"²³'ÕùÂEñgÇÎöÓNM'RØ.¢N{3Aı(şCyó&úÔü«JRo}“ößíPò7ÏĞÀi.“hÀi,®ã€Š~#Ë Ñÿ²t€™i"ˆ	ˆÉü=ñj†Èys§œ|†/<`f fŸ.ş#6ØÛÑÃ½—<Î"À¦}wÿ¥ÊØå¿z¾¢mÇe×uìÒÿO´øı›FStìZÍ$ìZ×¬ÛÅÀm€ Ùı—êyQ?V¿VÒ‘¿ëó( .op·‹íQ [ Û™_nayìÏğMÁŒR¤gìM?À  ??—Úæ±#_Tët#˜éĞBIädıŞø}6n#ßˆ @±ËĞİ	är‹Qdu?îx[ÈkçèÜŞu¦Å¤•±çªµã€äfyyÚT 0ÄuFFøşş `ˆèbzÿŠšjøÿ4Ã Ë‰pkÅ…]e_5h+¤U‰‰½c3èjŸ	Pf8ãŒ£ÿÒª”gPÀñ%çªÔ½#ôßù‹>jÄ….“xy¨‰tz“Ğ.’6BdZæ÷o;—¾5@Sğ,œ¥´Rèš}g ¿$Û…mÅK—úD8ZÚ?cÁ¹Œ=U|
°¹X»ŒÆÚ\N“ÏzïcM^‹ß:Å$n~^Ä¬Eä'vàú}E9g’Zùq{”‰„íúFû;<Ú­ìq»ŸgBô&’l7àÈ¿rF‰ğÿšD·ìÿY$I‚—àûHDŸ¦Ÿä‡Ø KNN0ĞˆHÈ;úwv€–+¹>’Yu‰\ƒ‚‡äƒ¦ğâƒ^£Cú;Èˆ@»cİxÿ¢ .÷‚€€ ’ ú[ğ£üş1ß! ¥=¬\e7¥=ù`Ñ¿eo¢ ²W³â¶l 2–ò“ªl`rIÚY[{+‰ZdyÈAkQZõªĞf½n¨,³Àf­µ,ò¾cEµ:•\”ƒjæqJ’DÁµÍ%¬¢¢.TÉî ¬¶òÚ£N¹ğÓ[Îµ;®ãbÏ@ŞUxëÉà¢¦ì$?Çuû‰k="Ä‘ ¦Un‰?UP¹öm¾²â\Ù1‹õìbË¡¼¡‚’foÓV:Ç|àáAÏvR}É©Çèõd£–.Ï#t^SL æyHŠJ^Ü*)D¢ A¢À(Yrï¬o8NŠTqK J™V©O©]îèïPãmS¡õ™Ÿ?L©š¨õoméÇx.VF#Ä*Œ#¿?]C¬.^RÈ—d&WÀ^’ÜI57¦,BäœŒdÍ¶@Š<£Ì%}ddË X©BftASfAL$¨¬£Ä,·Ä÷_aK¬ «{Võo¾ Ø¦ñóÀß±†'Âßáëñ÷~³¡ù5ÆgËï›ĞY~Í¸°/°ª¡µËŒá§
P Øäz¹û¤å§úíS éZáRFª'Ş—àG(	”Bí®Sg¤	¾€‘r	J‘5µ\8¾š%]Ú÷Ëiä\"ÀN`7èïˆìPD¼¯& “6 `°ø¯ë§--Å{›^~«×d(S]":Ä+y÷	ıŠqË¸kÚ7ã—ÈËéù	}‹¼K!#Ø§èW
 ˜Ä•,ÙïVABŠ|ìÇ^ OŒY”˜o#İd?c&×ªV PtT: J9È^ä+ö£Qê[Ó¥9,lqñ¡³ğLÚ	É^c-ëäÈ2æã‡cçşûwß_B¾òsÜÅÈu6ÆCbwÛK^1›óŸ#3ö¿6²m¿|@Aà_ôq5Ë#ıAäÚAFEm.Æl5(C+?S€€@.`7°[è]Œ>Æ˜ J@% 2¹_yˆ|$S@µ¸”ñ‚ôÈ=4ü™´##ê ıqÂô BÄDÀğ€ø@| ~aó|€B a øvèø~A¸¿¸=ˆÅN‹ß¯K÷™º'â¸QO8J
³Éxó„C´¨–57Ş–İ‰¯mÄµú™ãˆrî.g?¤S¿­îsÄrbâ¹Âı}‡zv”*µgHKÇxen'hıù5ƒ{±„rGûv¬yæéjèöÒq“ªÓX´w©PÒ+¦µjòì6o[2MÏ1¡ñö-ßÃĞcŸJ°ëUÙe¤4ªßÇFŠ2¸Mj¸ä"òù)@Š×Núòõñx‚¼SÑcG,àR}lRlÎˆ¹'KË$A!ªàÕê<ü—ŒCÙÃ”Ï©&xşºB­“¨ÄC*)c„_œ/;¹¸4zk=??vwóãÔµæöŠÃ­g×_leçaTpò32bo¨¡/8nxÉÚ€‰ê'Äï‘CÇŒ‰Š„Ú#ÿUĞ2ùY•Æ8,2½¢ÊDŞ‚î5AÈDî6…ñéÅbääßRÄ6–K‡1ÌÅïJŠºÔcló¯¥Xõ$iT§ıWGAa ı¦í±3Ò6Òk{ŸiS"€Q®³7Fú½Èf.N&˜x®©çN´7Æ-=
|mLÔ9mTÿ$G¹'&¡-BŸæo])ú[?§æ¸¥šB).â’½-Á9f7—e+ò ™ U®š‰›'€¬?Ëm÷’ZÓ‘ç­¿F4Õ0}ßn
±ĞDd6Ã±‹İDÔ!ä~úû'ËÑi4ì^Dñ%=Xï|çÙ–I ™ÀO-†»"fFC´ã¾õœÎ|¶Éû1w-Ò2†µé>ÅgÙÇ¤U÷Jàwéä%¯(Ñv$ûgGÑ¢gıODñ, »§iëæÎùœã/íÍfÉ –„ÄÊ\ˆã6ıì	N)E.õ@‹¦&˜à†-u&|GÍÅÖ	‰ûØşĞÓèåÃö=T[K§;´**"ÛsÀ…§Ì¢FÖ5¯¤ã€ÖÈ–Äà×¸Ğäa/ÉÎeI;œBıÏjâV»'{®
´øx}fFoÍ5áØr7Ê£Izë$õ[Ï	’H'ÅÛìQÍEÑ¾«Õ7;FÚ„—SŠÙ(PaÚÂFWä
oÿuW‘ßMi.Ca"U‹1 ~ĞÉëIä´ş†³fTõü•õàØÙïÀÖ—Ş8fº” í%ª¨æŠ††Ùˆ`1ğÛËàéƒ¼CÂ¶›ÌUDÌ`ÙÇ5ÌôA[˜U24¤P=yÖuÖŠ|Õ÷ú3úŞE_	á#–RDŞQò»n:2&¬CxŠ5WÎQ‚®5üöˆY{ …‰\øè‘k‰cÊƒq¥î
­-Çv¥È×vì)in<›GÄ„ŸOWøßùÜgâhAò­”õW·¡øu:mhÙ;Ó¹QùÚ<fm¡J&Y”ÌT.¨^ÃÙí£âí±¥*òà5Råx’Åˆ¹•rùó4y5sİ¼³?³c¾òğŞÉ_•¦Ú”ãÚÕ®¢ÊÙ”ÑJ(
g¯´ò¿d5jl¢6Ë)€e5&I¦bÍ2¯ˆ<«Së²¡€2¶ÿ¬t&OõO¶6²d²Gçâ½0iºoĞ ÿÉ<$ı*ô”> ÔoÜzŸ\÷Õ‰ì@Ãx#³Åúæ~n»_´ùÒf‹½ôÉ’w¯|£hÕB÷n.ët:Ô´¼Zÿ¾‘F7òJ!F›Ş‡ÓÆŸŞw¶¶Ÿj¨R2Q¡ë3@W©¤!ñDÔ*².^&i3ûÓ]¦gpıİ†8‰¤¥ä!±Š6¥AÈå…oÑîÜbÜS]Z¹n6ûL/Y >…„ÜævS_nÁİU)Ùœ!íÔæâ¥È»‚Éêuf÷öXb@S“½¥t)LÁ3ÑšC)ZkCEò¡ ™¸´
ºT–¹ÂÔ°DŞ7×S<éô#¦R¢»pâ’-)’ÑRbŞ¡ß<ÙŒ
«	ÛÑPú,F”²&ØÛ:*'SÀØÀäÍ5Ç)ñ9¶Èï’„´¢&UZDÂ—:ŞUªÁ¯ÅµÁÜ¾ælĞ[¸\üëÏb“*t]Adzı±È<·²D5>1ÖÓóy4wóD$†‡A¶X.GX$†×%iR·³£Ëym4—sê	Ï-y½Š0Fâ™™§Ò=ü8ä>¼8%!Ìe$-ğ‡ª¦AO«†ĞÕ#Ñ#kôóäx ^W]oònÔv´ó6çâõ$¢ù'Âü`÷şÆ5Ûâ0¶Ô¤è­O]øûtğÆÇ$Î¼SU{ÙB˜¶S–«·föÉ$¬2>‹å‘Me‹3Ï||…áCø–Q&Î273O¢Œ“&.äÍ¶³MÛ­‘.“ÿÕ¦:É¯ØÅ34Qñ×y]^èU$77áÅSQ*Êepªqáç3.Š	‡×áÅ9+ë&³Ôwsğë1‚t³ó“4É}IR9DÚ´ôvû˜ë%ÒË„Tb‹fÜÌ"­8ñO‚[ÕÜòu8!‘š]•»B¦xMÖCÏ”­ëYÚŠÓT)S]¸ôğ§Ş¤Kò…vwI­pœ\Oı%¯şB™,ñ™¢ #Ï2ëŸÄú6§T[Lk:Î§osIÏô(OÛÎ=ã%‡v	'Â§74Ã_ p•F@ó4e<€?Rp?Bı(¬¹(ËÜgM•ê®J`ç[xìks²Ö%V÷‡‚v×»ëj>dÉÓ,¦$WïR¬î£…no¤#Ão—cÏŞÜtEJä•æŠ†#îîô\ºh3dm‡‹½'–u!Rƒ™JŸãá¾¢œ/_?SV º.™R¾Ò¾I¿œ¤1,‰`¿qù‘ƒÂ/t=´ô7z¸@.?ç7ïR{ 
_ÌŒ”²5Î}è³#¹íTìõšËOOù¯: Öğ‚ß4uíÎİê{†î¯ÅÄn¿ÚIœÕxæ‡_¾nÄÎet*+Şüjä/ØSœ:Bîß.v(Ùìlàiìôr¿hõM{­Î‰ïğºHŞGGËËÙÆ~Mwç¶ï,»_^nĞ\9^¯”şıúsæh‡»Ì™©Úsê• ®‚~pfğA¨ÎîM¸¡EmãÆˆ«dºÎ4¦/[œ¿îôA·âêB5‚l¼S†c¸c|úåf´úü'«eP¦ñ‹(¼ƒYö€ôXÌº$H7¹Bö¨¨Ãˆ©!´Ñ^´<a`ÿ«®°j¾‡ZW¨;&êîĞÍMÅ‹VC¼¤e>Œíƒ^À–pWºê%°¶GÂÆ9“cº¿ó•,g¤««½4æ3uVÓÆ¶ÛZ«Hüº/ëaå2ÆVÑÓÄ $÷›\Ãé'•?+¶w6İ)”Ì÷ÆW²‡(Ó¯Î¢,ÎrœCó<LŒTôú*±·'8¦·ÄuorO{ú[mDôdÙ½) noã\?şñáÄºU¨%5˜=å¼ŒÜ÷lqg½5šÈ¬¹¥·ªšlRSÃ)óˆæ27ºéJ·}¼Œçab8´‰~±´Sp&M³F&T²R.*WIäjF[/Ãee6mØ$(yƒÔ‘]âÛaLjÆSS)vjQ³Î=y*´qL ù¨aº{½¤äzÍöH¨‚ú¤“|¥ÃÑñ©åwÁCß'ï£Õ6½ÊĞ½Ü8Æ}É;s•fÜœv{¦ï×¢êav3†óÓ%©§ıô¼f"%†àŸ@´À^Ä²Ü¯O>/‚‚ÓàIéÔO³vÚ)Ò ³CÒÛ5EÉôñY£n".	Ë3WÚ’7Æ5jİ„u¾ïœÚß0›æˆÒJÀ)ÒÇfœ»É@ıŸ±RSÃY.7i¡™]±uê.=¬éµL&ašSVV7‡*¾Ï×ıf^_ÒçÖÕ¦ğVB%Õ¡'jıÂ`<6j3\±Ê”äf­kóö†eNÉÏºÄêæ2×Œ³“¢^Cˆ¬™8ÈŸ†¾Ÿ-Í¹£¬Z(YälğÇˆl9sWşîÀ(ô[§Æ®´ÙŞ“t…_Ï‹õh%^´7&\4jqì–_§4Hìs×4>1×ä’<¥J­ËªDâpó·<EqÁê{yÏßçÔyÜÅw˜IºÉ7kuy.tÅÉV£‹ŠÈ´ñ¨.„=§»óMæ­i{¶/X~ÀÿùÂmxÀââ[Üˆ§ “<J!Qä2Ù}qvg•uÏp”3å#V¬Ä•g3’@®¨0#U‘µƒëGÁ q~êÏ³Ê( 7Úyw‹ë+ğ'­Âq-^ô^nÕÚ³¾ªX‡°¸âÈ
q‘şOİ †CY¨.Š¡øÌ^.¹[ìO•1ì°ÚoÇ¼¢íY
9ÁzF î¥©LÑë%sÉ?9_ˆiÙüîPÃqÓ¨sTæ½D½Í	P¾õu¦,7ş_ÃØ «ıS’D¢¥œŒE^pÁÅÄK¥L¹EÉ,ÉJdíC—MİÁır%«©Y¥x†™ŠğQç¶*yò¾ƒˆ®ïš)…0$M%~h»¶Î5Ğ*°c¿g^P#ß!ô™«ª>µY©*½EB€1aÙ‚€4pÏ2wS’*TÑ†¬•»|lsÜéŒä«ñÀì°Qí´ıâ úkß“ï´h6$á¤•TşĞŒÌ
”’O¥Ç¡°ÀdeP·È ç†®‡Æ¬†d"—3‚G´K¼IZe˜ïÏI7LÊyĞ™ËªGs§İï”Ñ›
\ù#]?`-J°\Iˆ\RšñLIy-ì¤­Í„Œã‰/,åÌ 5[R;?)wM$‰çã{ñ>ô±¡2ê5¨Å_Jkã"B@ß@ğcs‚®õú@Ò=5Zo0úQ6ÿi€Ài%’=	ü¢á³(†°‰êÔ(PráŒÂ_É0Æ0k£»6‘qŠfF½jt:Î¸Æ‹mµ7Œ3%½]¦ÿ5S”ï"”%0ßTæ"7h¯Òn#Vò]·YÖÄy%lØ]ÇRH@íw/d‹îğrŒS5V[Nşğçd<èıåòmK$hœ“]BfŞ.!¹f¥:»öûÜ|oÇ&Àã$cŒ>»áÌ3f)æKëeôBlõGf¯ü“ÉÅWšeVÉ6KœìÙ±³Çİ÷3ÔÈSÌìSÆj£x`±G"ƒ¸S®ì:ÉÜV6rÉÜh¦ò³ö¯¾"zš­´Ù‚~‹$õï‡xIpr'Åƒ£ UeÇFhÒ'öÉäQ.İeĞ·w¶Rt[6²àú‚ß”ê]‹â)ítgÕ*3Ï¹_¸Ê™İs7dNl„>	Dß›ïiŒCœQéú˜¦eËÊG­Â˜½\”¶q·I%fh-T,&¿”bÇçvª>­uÚï:Ú |YÙ#J†{Nû¢A-ßFñ÷Ìæ ôu•\šÜ¼ÎÛFSH*ÄªCGğÓ³6Vh8i¯HOçA«ÅÁÍÇÎf‚œÎ’fTÈ;0d0, Ò”Ãß·2ıçÒE^.U4İëJ˜D?§F½Xr gS<Û¬]qÿ#Ò'{‹íiQ®(æüvbhK×{·ºÈîe@*sk±]4=Â•Ï@â¡w·.çJ·¤»TÉ^˜M=µ¿‚¢mº¤Œ€Û;³ë#'ÌîsÉfO¹ Ôçú¼´¬^4|)•£>CÇp¿ğ¸/—WîÄk:·LÎŠá’é¬Ò×-5ıqóEÛÿ‰ Éÿ–ˆ0ß2E‰.š,tÍºT(rí+•,~ÈG–:‰%Ô`é¶Ä]QçÒ~’O#ÒZ¥--;rª¹C­Ï¥<Â;3'%b”·å˜Î 'İ¤m—uÒşíj)ñ§f£YRb[=Áp¢ÌŸ„&b6ìÛæ7<Ëğ= Ù&wô0–©Xu#N\nÈœ‘ƒ’ü%à;â=68¨eŞ7ƒ®nEcjìê¬¥7İëÛvA£–éps:üó ¥°ãŞ‰2YF¿rÁœx´Y~µ ı­nø»~Z^'ó(ãf[¸/“âİ<béh¨ªUMBÚD«·PØQÍ ¨ïÆM¯/+,¦Z.Šº¿1§_hykÔ[yn¿Öª{8[™Ñ­/×÷ıƒï{&RGäí‹}×ñï™Ó|óïåWÂÁeLávà+tt~Èò??óu3¬ …vÃ"mŠ=m#Z—k^ş™~Û n¬ã´ÀXå
«¼8¹ª§<â±äï°?$â?S‘fzÚşŒq0Å®jğ»œ<xs¾_®Ï½‚hgi/ênòà^w™ãvßäÔ•İ˜ñ^b*$$/s·À
Ç_:Åó¤ÓNOÉìƒÃ"0•È ±0íÉ¸÷¤ú÷ó(Ø>ı† ±ŸûÃ¥ä<—¬ß'(‹AU 3õÚam’4”=8·>®)§Uvó(Q÷<‘
§Ğ‚ü Â»	˜0}o‰Â'¹¼#s´Y¾¾"wø¤0Š©´»fŞÕVéÕ¨îRê!şùx<ê~$¢ì¯£5T „¾®CŒ2¢N¾'©¾I=
øğcXW½^ÒnØ‰4¶[«6Ñº³x¶VKID¹XŠˆÉôMÚD¶}¨-_c3P²>ÜG~_ÁÔª°Jw&æjfî=9şH)ø&zKïúïŒPoŠsš/ˆ&{*»W`¦š)¶í9d ´tlU p )b€:õµPÃ
[ñ†o–èúr ß¢î±½x]É~Œ“Ş“	@ïŸáûÎj+6‘¤¢«0S8Ì²÷65…zHDÉA‡üOÎ§¸MÏÚ—hhs™ePT¯=Ÿ‘…<³³<=U+q‰Dê¼Œû
ÊØNçì­ãë£R&føkhŠ“Wâ½ùIMÇzÂµË}„à–ï4 {*]°`:š©”ğX]ïg·*Á$#û8KŒºòÌ·‹o'‡æ¡1õ±:ğc‚¦"aãÒ«Kxß Œl¾ÏBÏÄ´Õ’Ş÷Üñ»6pöSoï%&_6:ïrqíÖHşdQvz,=é8Ò†ÿ¸lÖosš©#í`<”ôüä¨e7ERQ?»’Zç˜Ü=ÒİTÊC(î~”‰6£^
{M¥éØ…„Hvø­°+×¿ûVóÉz¤n¿“­Â´¨S0î 'œÌcK?Q6yÉâ-1ˆ±{¸ œS¦„ÀdØwvÑQ K§ğ2é0u«·<]eÜ2ÑHµü%×î|¦¤„—¬Plù¶®…0Â¬§<ÂŒÏJq€I_x€‰nå>øù-ÿ‰Û½ÿÃb™›Ô}™¬W£Eµä3Ô4(İ¥X©‰V-&ß2¥âfei±÷¹ğ´sê¸ò¯£Ã¸º·âÛ£6M²“-*±Ÿã?—½÷lè„qHM~gÕƒu¹[f6/c±ô;ftİÀ;fˆA¦°Kú•3Kz•ºú€h«*=CP,ªWÔª]¨ÂqÒyÃˆºè(Š
ŞÖ&šhss©IÜ¸‘¦.”…ù4Q­‡15‘{†”Øz/‘&/âªÀËâ®•vc¿˜C·Æ'‹¡=ıåá1£¹6	h€´NöœÉçt£.TÑ*AT
âéªéä|ÿŸ¤52Œ„Ş’D réıªÕE,€àdS
_ãßq•ù÷Dgù6$íÕE8Šf”>DÊ¸LrÖén#f05ñX"“TK¿wÓ+Á“(!ôR•A(Ñ³‹©¯”ñ“Ö™íÊKĞ¶‘8G6z„Ï‰âç^:§€^w'M¤[wÁL36éˆY6>ÇNss«º·ÔWJ8›Ö\bŠšÔ\Şç¶70ªº|İöT™ÍnÈ‘WÛÕ˜i¦o}§m9fšÙ ÙrÆ4Ê@J+-moü¸I  :Ù¿bQÄSèèfó¤‰?½É5¯ÁÍşiL]VK8³°4ˆÀ¨A“gƒAô¯4f¶- I{Îèü)$8®iPåÑ!g[†‘+7j—cá±ÖÜi$¤œ‘ëNl«Ñ÷*×}@tOàE~!*²â¿Æ»Ã¾Lè²[¹¸wDö¿â6Ö¬Uåü$œgÄ¹k›ç ½{:Êo%+UáoÉŒ8×ò*]wÈ¨ÚeFfROè<Tó\g‰Áç¥Ô††vh½84.æÚcÛİ«h×ÿ.¡Ì~>HhKx
ğrS'¾Y`Œ¤vœn\r–J® •Ê$=z¿v:Wb§pC³E®DÓÄ­ö.3oàä‰'<6ĞßPlRÒm(Ø?¥áÚyöî—¡ŞÕEHQ/ê±ÙiÀu.µØå-ú¾®¯b~R|AËö´l¦ GU»÷2ÅŠ•†P…¹ş£şUXmµÜ´…vÊÔ&Œ_18{h]/¿t…MŸ}­Œå—P'J¤íRpåC€{Ë0#ó+¸Ã@
&õDSÍ]•JŞÖZ’qò“­SÖNıÂòÙUÊÚ"cT”‰â`eozšPC,eòü…uÙsù¥Œ‹	¹pR™úaÃ|0A	$¡3¢P¹=e‚ĞàDøA?ë@²†.Îpóˆ“»¡3`}LüsJVjy£0™xrKRüo‘‰1¤qf1Œ‘qìâbÛ°AøQkÄPùxÎìùÁJ2Aàn&;–Ç•ªx	¼SzÄ°Û*Ğ‘ÌSe$ñfĞ†¤ëh¥Ãs4Í¥ég$µ–ÔÔÙ¹&Ğ‰Àdz@ˆHİFEü^õyÊ½ˆLZÃ{ôDÑZ«{>}²=÷ëõÜ¾f(^B…0¦Šr“!&Ó¬?‘õ¤h•‚ØÍàÅíŠu­[H8µ6¾®UÁÍF‹$Ü‰ˆLO>£V–Ï‡²Tv˜,Ä'fIèŒYä£„`z(0³Ÿ/µæB©¶Xn/+ÔŸ§×3| €XÌ `ÀuqI‚€"Æcóüõvíªò $g$“½|êÀmÃt8DNnßgı7uf"Î{¿tì2KÁz©,îjÒ.-ÎúW·“±¿t“–
9Æ$ª¢\30w‹Q:\4ÉqµE®M3O^ê„ºgßÙ³tÜ´¡%éˆÓ~ìr‘ä¢Ä‰4÷&_§È7¬Ñ‚½6öorq<nY3[ïNUñ²ıÄÓC§ÎÂ+‘ÁğÙı‘íc“ë¸!vw¾¶Ìe-7ø;µËA`C½9¼–™¹§,!ãÁ’Ï3w{1U#ûŠ»pJAtªÇ–¹“å*ü„á&la›¡™Ğ<ZSÓ·m8¦y‰Â0ŞöGÀ9¬Ftš^¿Ë·©õã¬$WÅÈ¾½¿]Õj÷}Ó\ŠOÛ<ŞD°ŠÆpg¥Ùè«Z|—øšÎ:±ÃÑüˆ˜C¼³š·Ã47ãr¶âÎıØ+ Ú}%¾QQLÛŞ-&”ºŞ‡áæ<›²ô=ğìß½Á¸ŒÛ,mœRHËx~ŸàK×ÜÃáˆŠzUe*©l, 4
¯ª€ày`½™¿b÷:E¥Éh\!ú¢Ù25­ÇWGol1€&DfûŒÜ¸ºE¡ãT4ùçúœì4ZÄã¯9o
9=âæ?~Ûà.£U?‡uQ¤vNå‰„»¾Á`—lšŞéODªY|¸£ÜèĞ÷7ÙšrfaúÁb,\ZÌ›^
/ÖÙÎ­NP-ôáGpğrÏ×¹›¥ñÅóx°ã±$Z {+:<I/1ÿuhëwb±9"Ç$+:{Õ<ËİŸ=°‚)]1òŞÀŸ@€Q6“N>Óóƒ\ß7àq¼˜ÚLâË~è6£„î²ÁõÌŸ•ùØt–Õ*CôÃ´™4ÈL¥e]Q‚>ıÃğ„İU±h_­ZÎ4ĞêQTl}+‰õ[#ÿr8ˆ +}z;¨·Ø³Ë¥R¥eúÏl',
@ˆ8:‡<Gƒ´lÄµ0’47`r.X€%<Y2réÃG ƒè0¿ÜÛô<ƒEƒ@ßeU†/JíZx·ˆº|>1eƒ§S¼æ™«o÷ÇBîõ·”~ ¡·"À5:¾Û÷â]mD%À9P_İá¾©Ú½õå›ñŞèÒ‘O:É³Ãq¡eï\Q+ßÊº÷ıÕp–ëŒ7C½Üv“=P&NfOÖS}í›2ì.}ôE$§L(ÆÁ‹sZI´$÷öáÚÚ‚«WÏ%á*¯¯ÍH¾iU“‰a9“ƒüûˆ“G¸ÎrQÿÑc¦xÿ3ñûRä—ÄÓ©ß¤ä´–óF!¿½¯ÄÉ&œ²¥5ªæØ¬{™2»Ùaf×=¸»(ª/ôxZMıçv£ÀFv£:‹
æ†ğ‰3Ôlé
Ã³5‘>ö±íDzVj_ÑÈ)ƒ)³ÒEuÆĞ¿&NUEzcÆÎ¹E2å 0çl¨ˆŠõ_.áş
lÃ…)œŸpâˆŸ›\ÏßõUÀQğnëÍŠV\v(÷+rIÎ3¡3eóÀX$ŠÁ`ó1c‚¿;“ªZb¿Òğ[S%lÏ–z'ø|ÅIÁÛŠûKĞµ³«Ôµ”HöUkÄmPë´ÊÛŞßí'u­ á³4ÎâA¤“ò–ùC?ñ	‡Ã…xg7…ÊêJ(ÒÖ	,Jµ	S]/±G(GÄÓ?}H;x‰µhú¢ıŠ³ÕYÎâô"ß6rÿëğß¹m¡ıõa-nk¦ s€†$OíŸñÀ!İ÷äã3o~A˜3 †’7bÙ%²P)PåÉw½~p\ƒ´á¢	àv Â­°ÛØ}l¹ËâÓêê$ÑÇºHz¢Ê­ôŞÄÔK½~*{Üõ©ááøqµ_ímmSQò¥J É Ña+BsÎ°Tie…ƒê)wié+hÁ˜,ßa€Ò¤Ë‚°õóeô,ÙßÓHAôÚüÎÎ>r13TOš0]ŒvåîŞ/Å×¹º{†s90ªÌ³‰éîç /ÁW	mõCLlâª„|Fq†÷*ZEyAq!}4ƒP‡+ŞIÌmÍKüÁ†ĞüW¤Ô¿)ÕöBğLs€†0Iâ9ÅmH9†C Ug@õ¤úBÌˆí×?ÑúLÌ½áÛİãJ¹Â)£Õ¸Cs1båÛ}S¢wÎÒÕ¨JtŠœ˜çOÄdèwÙéªïñüèë‘òèÄY7‚T{‡Ğí²ÅBr£I“¯¶yÊY¬Kiì³§Î©ñ¶j@åH/Fµ9İNh+S‹ş ‡ËîÔªQ1£Ï×5ê]ÇÍQ!6Ş“@¾¯D³§‚5–‰Õ¬Uİ F÷‘è*‰ğ‹nw‘•3˜¼Üù¢
Öº©	M- œ¢Ağöâ±\¼ùV]Ë™9 ÙÀn=™FpÖÖOµÅ‡×Ølp•æ)¼´]Ë@sÁÂ$MA\ÀİÌeÙ-b`~ñ»çé›ôÑJIá\_çJTÑõš1n‡”€gvËí6İrF»Ik‚k¡´¤¸öäÙ
wêq¶w'øìäAx0ŸhF}¦€èhämGî=,‚¹¢?›Û×üQïG¦ _¬á, Ò?z9ş|,ÁÓ+vÀ’`ìÙè¹­²@Î(2<*ñY¥ôN¸ŸU®®&¾™/PMk……äÙ¨ƒYX¿±”ïhQBh»ˆ“{'cÚ¬«ëWs;u:båR$È‡òGYÀUœÓ|Ämñ„-ü7&<îbX“WyI3C%<o3ò+.ëÂÍ‹ÑöAfÛ|êwÀÊ·¡ÆÌÑu˜Âcqx%€m…‚\@"¶­R{8ehÅ‡†xe¿všæÓ½³ş¥Ş†j7yPì}L¸ä¯;¾Mb©…?Ñiã¨
ßŒPØ(“çåIØõ9¥*µ„İ%šöÔË™Œ}İbtZ ÆŠ:T¨1 °#*qõÖ=æàE¦ë}ÛK¨5ø}[z+’ØùF(Ç+:84>¼{Üx_<2ßîÔk‰¶éÒó4o5~VÆ¨ªFíöçáTJ~¨by`÷ÔBBÚ„Å5´(«óPì‚ñüj³¸şh˜±9àå|!1Ó?“*¢Šp’µgcR•ñúˆGSÑĞ4G/ì'wrZÍûÌ*›’ÚGÛÏ°é¶§ôµAm÷Ñ–ÂÏóš¶Vù™Á¿?â~dGE<-4] Hüfº´ÚU‹ŠO–Š!Å•f¾¶ÎúhÇÒ:“»­øŠvXçñŒ¶™ç¿„vYôµO8G¼B{¦ìùJ=´%DiaäYãEˆxùù·b3•ñ7Š_“M›Öjyòœ<_£Ñ^COñ>ÂºÀá}©úñ¥—Pô=ÉµèĞXw×‚bú‚mæ¬X´k¶øvwl_ìü8MÁ€Ğö7³ñ²: ßĞóñç#
m'sF½±¸¦:/k+º9è:ˆßğ³>W1aÔ&-)Ø´ZóÒ‡²ˆ¨J9²vÇÌ“ñŸY
Liö!Æ/loù
€ïS°û Ylùä—IñƒÂ¡Nö5çsÔ6SF“)ô—öíì¼­Eé-er®w›rNŒ¼üñW¹ìÌœ({ß"
§üñ~h—qÌh€mÎÌX{ÖI4îS~~ˆæJ4¿Ëre	Å;+X`áo|8áCÊb¡Òc‡´än$A¾‹ËŸ¿+%Xa z(Óm%Ùm'ö²¦@·ØÎéƒX÷5¬UıpÿX[LÌ8©µÔÅ~læ°°ßö&¿«MÍ+¹{û®ÉˆÃäï;¦ÓIf,ê†íF×·îH«UJ>‚³ˆõpÖµ—e$ûƒ"çà>S€ûğ"uÁÆ­Êé;é}äïÌŠ? ÿ¹ıq_®÷“u¹Ş=ş¾]ú«½æÌÁ-Ñ$Ö-P&Ô*WÆnä“¶òİ •'YÉ/“ùÅ0^9õÃÛ“#çsª¢V=ˆå4l]TI8™ô³çU³d«:‡AUl~&Æõ—ùúvèˆŞ·MpA–Ç§WUvtÃ@–O—æÛwë)¡SLİç ¢ËÜò&â‹[K%ÙâHÎyš¬gÂdÜ÷cëf¬›wæW$JTqgó&ÖÔdì²õX›8¬YóèX‚ª4R!€	GåÂ´ïİA4@½€ÕÆ	[¼/0Ô¡vñéË¼j—;¢Æ«gö¢½‹TaÀn¹àší'<7Y&Dtúìõ¯¹!]HÔ=Ÿ=V¾Üç×Ú;"á&Ë\G5_š[ÙŸ¿
[ŞÁ¯¹b=§‚¨V+t1æÈ±nH¹õñíœÎ-D0*xò·ˆ…±^^•àšù8ì‡,"^½Òßd´¯„„¾:öm†‚äêoŞÃ‹ÃLp›ÌÂÛ«¢”‹ëï¹ydÕÄ<ŸTË‰K§¯=3o?6ªZ>¯k‡ì¶¶â•Y‡­÷T	‹˜6}ÍëöJ ·Ó€À£-¤3ÇªÙ¡*¬DØ.7·³•Dîæ;3±‘ËÄÂ¦¥H#Jûo'Ù¤ã;rM	Ò~²ˆ)ÄK²o`G(Ö¾aß/S¬{¿r¿P“CEc³ÍYMgàDòrh(ÔZ†ìı³aï)ÚÅrk¼ä=ãñfï•Àj9"Ô¡ıàhRÏåìLåÉääºä¸ôÉ§áÉcğ“†ıF³?òÛ+UÕ€ë•æ¾Hô`@–À):oüû»WCdL Rh„ê­x‚hO
œÎÚ€óØª£&¸ÈÉGŠÓÒ™&ş ıÆiH1¿ÑÀ­ßª– 7Ö•Go$à3ñ–Û>^îşšÿ5÷…Ïd3~£j…l¬ø”Wví·MB‘ëRWv—4yMWï+QwúèÆ3Ì6‡ƒœJ0ˆ–StîUşYZ¥¢ğÆè)ø½Oø¸†­!zQ]±µ¬PŞC¥Û¢T;F¸$Î€$|•
¤$Òøíİßß~Sà™¬cû³¾ÿçJ¦[3:øËšjñÂVc?sßtºÃæWÒt<Ÿjî`É© 0,éLÓò3Ç­	j\Å¤¾FÈú7Ñ¤¯¸ª9Ø¥÷5DvhÎî)ÂvòÅG†Ì¡İ)!Ì\{±pd±´¶±ßìî”¯;=ÖérœUm[£ŠcÊ®&>¸äW§Ô*õ’4“H ¹ßå;¿9¦èiCÚVbMxİL"ü-î‰f@ìƒ#ò¥[2][|‡[²“\ƒcÀÙàá3åÆÒ!Êâ±³X|>ĞaôÓˆ§Û­ÉJŠi0“=ì°ÅŠÃd’c¤ù6´ëşSCºğ#Rí[”9.ñf|¶Ë‚ˆríö©jÄ]u­1:Rm÷Æ~](aEøÊ !+ë¶}Æúe=³a[÷ê•@ÛÔš#>‚|WË%?= B—Í%{œ…æ3@>–ë3v„©ã3¼ß–Î
	±±Uùa¤.ÔpÍ+‘`½Rz-œ¤ß½Níò[«à”›¶ºÓnP’ı
Ä+†ÿØÕĞ54VüØ=6eOèŒ€˜Lœ+4*§ÿ+.ª2Ö_ß|ê•;~­ÃôùØÙ1YÚØ0­Ba²µbµV9§ù0ÿ&ş8X\†0Fø‚QMè<` $WªpşÌ‡¼èëÄÌ€Â‹ØãiÄÜĞ\d¬ÁJ^:ex=ú‰–oq*9Õ×àğ]¦TœÁÚØ~*·?a!¸ê}Zi1x#rå³ûŒÕ[ä§vG×¬âÆ’ÂVìŒ09œši¼ñºŒ"WãsmWÊŸöhïêØg¡'Loig6f9ã—g–(@\4i‹v6	CeĞ= ¸ı‹[l½·Ğ4i€u7˜M[t˜ò˜O\RÌ]cŸ&zƒğ¸µ`^Ğ¦<êRÕ+GtMü ­·èG??+?-b©DK2¿W¥Ğ,2"~wßb°àÈÿÎ6MœÇ#Y˜$ˆá `{Ÿä$YğfÈÏVúÍ½%~T™pv,Mš,$Š…÷Áb£äúTG,YÂÈÓ;K² •‹go:îÔ>æÖläqğ@4M“N{ôd?ş8r¢ê¦näAC7*yö»ÈÅwÎ2†×Ü­³è}ÿgA‡®¼Ds›®|{ä>`œÆ‹c¯}ğp„Â¥Î)rÜ9GZãš¡ÒG©sü°£3‘Ö5}SmÄ!s<º>‹iËÔá•-{Ñ†iY}„ ¥E²å¡!û¬øû:ê}Ÿ;ñ—ÆşïD;Œ}2$¸i¬}l$èJŒK$˜qŒëõy¨pÃ˜&óğ'í3'Y{Eï¦/-i ›>\òlÒ¸ÈÖµiò…+Òs©Óä·ØÅ<{èåHÑ }j’)¨Ğ âŞù~ş J2ğG£½Øå^#mæZ0Ÿù¬»1§Úçòª† ce.z¢Bo.-6km §ËÓ§­&½û.vë:?(S»2¹§¼lÕróöÆ‡kÅtoR«± ùV}ÁŒ![j5îÖ¹£WZ‹Ñ@È?¹Â‡Ôªì;½ |á÷ß“b¸Ÿì-Æˆ¡!µºşeá›g×–ÿ1†xNR‡Q¡ë´1ö¼ÿÚ7<©ƒ(æ±Y<gƒ=Œ=Ó3Sv¦ªólĞ\/Òÿ¡Ú‚sá~€ßv×¶m›OmìºİµmÛ¶mÛ~j›»»¶mçıß9çb%¿I2³n2k’L–„ñ …,v,L±'„!»K9[÷áèÿıu£™¼Á‰úM·ÎgZ­LCPBîÅ%;æöÜ€–nÃgc+§D^²'6‰~4aó<Æä‰%µW$™õöš°Šùo7öú×àJØfR†§£°üˆ‚šIØsš	à§ÉÜuöbiñØGÄÚzdEæWoíL­X„‹±XOIÀA[¡$kõç¦Ôr­â yx%êdãÀyø36qK‰¾RWˆÚúù]“ÛnYæ;0W¼Š96–&zW¼ÑÁdá=HX.ÅöÀÁXRv±½QM»à|
XfÅ !PHXfî×b0ì™ŠÎ5tlê~¼ÿ¤ÿ®eĞ=TŠ`Á'èÒ–­Ò÷-Æ$‘bñ$Ôœ½b_À;„]Y…¾Ğ×¯á/™Å>‡?á–„şEE)æ|Y‰ÑùF%© ‘_è¨	óŠÿqNşÿQŒvÔäB$ °0Šÿ_À®5*ÈÄ‰å/r")â·-ÛÀC°	Bâ7]›€ÿ*‰iêÿuõ,ÆÂ;Tt6’c>Ñ‡	-V©BUé‰É
-ıGÜ³Qw¹ûï@ûğ?AãÀğí%bfÊ@Æş‡PÜz‘Wÿƒÿîsàó‰ËàF´ÑNAj°eÁìÚ:³A l²ï@6ìÎ¶F«Ï&<a,™`ÿyğVİP@ÿÃ\¾ÿ…ƒmâ6\‘ıÚ˜%4Ø³ézÅk¸JşÏÇüÀ:‚Í†||Êd3¼>ü¢iÜ‚ğ¬fÄÓ'ÁÌp"½•ãéôeCwN6¿î!áÆ±ìQzù¼Ç±Née
ê…ô¡„ô{n¡Şa[qä8²|]—#{Í€WQÂRÌŠ1,¿wÆ-Ï›<fmw/LQ1Îm°Á=7Û%ÑšÓ–`óp%XR7˜B^Q-ü%XÄÕÔD5Fô M¿LÌ`,ƒj.wãzˆ~±ç²h8·YÆf´€&|MæÎIy72§³0ªCâÂ°£ZÏïƒâNüı?:'Uˆÿ(ñ+öPs¡`¯ûŸTŸ\€èÑ¯Iˆ”ê.EÆ¡UaEFF;¡ ''oğdVUt‹Ş³Y—k©À_ ¿`]©}@n›ÁSÀ¨ÍA¨¡¡ü… åG ö»–¿ºA=ÀnÃTÆğ¢¨SÀ “úÃ“@è‘À„b ú@lÊ‚b:CMx—eåpéN5éŠ$äH:Nx’Ÿ$N›Gšp&­‘7ÿÇ»?>Ék¾ä±‰ÛG¡Œó]9{èR©Å!S‚‘XÆHÁhGkØ
Š Ò¢T MÆòaÍİuìCòÄdZ€49æ›&icÄğ>H¤89&àõ%EClQ³Ë˜!±Ut¹nÀ%,0ŞÙ(4‹ à²+ãq¶¢i˜*×©Ød>™…Öd6H€¸à‡£t«'ºWW ıD¿W{%Y£9,ÏSñûM«.¶óöÛŸØØ%‰Ub›Ä˜|ô…MBTTtÔBV"¹tO6Edô´Ã<&avÏ<Òq VâÜÉéo[è?Ï"5FL°'ì¾†b"9ËÉ;˜Qbß+LXB‡ä$ÎuŠGëÄôó!FJÄŠ‡4)“÷@bTXfBN…	Å­O&u˜+xßÛN×êÓÿgiòüŞ¨«~­«cÒ‡¾úÀí^¨+<İ”N!UF“?0IŸz5'û©0.ç,A6Ü×¿õ8$6üÏñš&~’^Û‹#B=¨gîèn#Nõ:²;¾ÜV&ŞàôT¤9àö!{†ÓË‚á·‚_ê NøP#@''N+?tşvvMòŞS¡Â‰9¨p^Á8Ó/¹CÔ«Oı d&‘Ü	ªüëÏ×H^Ím#ú©g;‡}p‚]¾©êoÁ	g>Öœ£ÁªmŒÕˆC2yó;º1v¶ÍY›qRÒ÷‰Øœv–Jp’*Ô)Gü`zC9äÑşïœÈ/@|@yÊX-_*z}û?­†\À_t5¿Yëß—Šc‹‹eCºÖfO¹Ö#™ˆ9gsÎe`PL¯šû»¡fvÜÁXñ·û¾d9A»v*ÈóåÄY¸‘C0ø>ÜÂ1ˆT|C0›œddêXÆCkØ,œF—f1r~;Êª·E&;ëqHyÄÈÄ:¾ÈĞéùåDĞd"b!5¥>!;UÈí$¢»nIÙ§£ü³J<Øüø³˜ak/åòèqD9¶Ã–VWG õy‡ãà¨'uWÈæHÊÁO‰Ü‹G
¡Iµï¦wD"úoÅ…˜ıú¨'“Î?Ê?,ñ:\~%rß×ş…¤æµÉ‚\z2ãT RJÏ¿Ñr.8Š¢rF_1Y£ÌæÆK¡K¦ÖnéC:io!&múc•ş}z+fWÛÑ9Â¦ÌÏ”cÛ‰%­ædîŸEw"Ÿè6ã¾İ!¡ĞwÜşÚ*ŸšsŒÛş“š<¢9qÌSFZ‡Øx*¬N’hêwT?³H×¾€:C¡7©ëÂ‰¤›ØÜBñárÎmjp5İ’±¹z†¤s¿|àÖï‚eQS†÷¸Ç>]B%ÛGD·²n—á­BŸØÇKD%kÊC>“ÑD®÷ß¼ö½¡Å®ešLG…¸‹$ÍúTeˆ.ÑE7\Àºrº­:òyU²MöŸ¬EªPy)_y?;
ÿc°óÌf$ÁB@t~R°ÌFŸšu/;´oÍ6ër‹½É÷j$&8¿#éEi©ÍU8BîİÖÌàóãó8™ÆR(Şp‹ÀHB(0«6[éB{é¦[÷QV‡qI{_%Ö }JdwjpÉZßëñaz9Û½¶>#`¼æb¢¥I”äêeB:Í÷«ˆ¹‰—VäóH”ä”ıõô†lqN3ôÒ}œ‡fønštñ	©«´ÕÑ/äÖN„Wºj]_Â—œ³±·3¯¿>^ÖŞÒ€.¡H5QE—ZkÎ”ê4ÂA•1Au¥p-à·$E´u§O ‰m¤4~hÍBFÚmÄ°û¢3%œõÀ}²
¶ÒvçP®™”;…¹Ô:›hjGI	ø·Û’I'‹U¸v^&²ä¹¿şm¬»8{]Ô'e¯'ªşíş[İë•Ñ[ãÂÁ…[ ëµ©X@?åw«8Í”LjŒ®”;v¥gş2Õ‚!›ÚÎ@<Y^L2óß³ã[ë]°b¯é ™ùÌw”ŸJ5à(+|`4Š¨9-†_[peöÁšPù˜Ä˜¥%îküñÓeÓĞÌ)ëm-j±‚-ZYzHQ	Âê7jóÅÊhÙœåÀˆúrkØuú^Ó©W·mÜªÁ®–&ëœ°èÈ>6Eé»j°èŸlz§ª“™ö§Â¾Z½+ÇGÈ©U•]/Ûzjæ7ñN=kï˜ÂT”%Âö¥fÏ[Ù‚3E—XƒõMwÉ%)Ê¼pR7‚õ^ªV*Ö§àÍ®Y)V`b¾Şı¦•1s
5ƒÅéIP …,õX¹â¶>±{ŠyyéÊüëQpîÅš0¿°şäfè?_u²ÙBÚ×G¢	Øí/g¼¯	–ûÛg™–óÎ¸ö‹Y1$c<ª`_¶¥?{ñ;\º6KpÕ_Å.«úg/ö—§ÎÔÚfºäªö<‘~´³Môµş×IÁf,& ¬kù§å)ÊşÍ›æ“=¯o ÜNıÈŸãä3>ã'†·Â9­œy1PÍ‰F›}Şô†ïp	Ú—•/^	ªT.õÖ²n¸4ÉûÄ…Mêíµ¶Ç+ùÂ7/—?0²''·€–R¬7µ_û%€Ç.„;»µ3ä?‹ï$ñî† ©YPé÷w.¢K—G…W9_âÇÃÏ&ˆñ¾oşÕu¤‹~;œ„/£“½¦‹ME˜î§Xë1ñÛ‘©Â—Ğo»¾* 1döÔ ¸Ÿ6Û8ÂMB8HcÊÊ’_+i×i*–D›ËôT†ö=³ô•ª#€’Aí9f¨ËàèM3v§«_35‰˜>¾»2ËFÅêO»äS_tP,ê%‰ßõb¾²1VâĞJÄbˆÅ|r‚x2ôfÎÂyÏÅnƒ/-Ì\%û/¬:<û¸IÑ„(û¹zıÕ¡¾Â¿¥‚^¶£®ë\n+“KrÄŞÁ÷’_BW_Ïš_z«É…RSJÀ× gËqÅsèigÆAxW-ãIÿ’R…oú#dH4ê™€Ú¸O:¯]Ş#ª:T”äâŸÒJ””MnÌTÚ.ö•Ö¥Ö‡¯®âSu¤½ÙÃæJk½Ù6\„ñ”Vy„Ñ¤}|òR}ÅJ;­¾’‹9¹¸~[1C=ù/Ù#{Ş8VëkÄT-mŸd·>û½^s$°Á„môş2õJ%ÙëVh¨¸‘\uX¥T·ªc»BÒV”Ûß¹@ËˆHšU:[FE<]œ¾?ùújTÔg•çhÂÉúeä»ú¡WóXEÜrå”ÈÁG‚‡¼Mß²K›ÉjUóN–áÚQaUÃ}Äd»ŒÅSıªÑöÎ¤ƒ:ÚKÒµJÉ)Æìmì“Â•“,¹g)Æ<i<«^òİº¦r­ –Ihébñ "twœò‰çÙş¹‹×˜5K1"±i)õu3â'ù?îÙÊÆ¾¤óô`½Å±ªº¤4y¡«"*B”æDIúWO{õõ@ê‹áöÒ×Xz¿§qß˜=ŠpûÔaÅäTÑrôy” üdéTıºµûIí·| US
Mä(½WÒ3Å·ÅöCsjæÑøí2ù+2dŸ7SÑ?àÕ&-O2¼×ç3éÈíËªeÿÃäc»iGDîV/Õ÷·*²nÍÊ@Ï~t”Æ·hË(>Bªcu¬ddß|éXJk2ÆÔ4¹4ÒŸa}Ì1IîW,bË	Š”ö7,Úl&Áé*’hJ”W¦+¡µ.ê‰{¹™¥œôA°óØ¢3ÿ;Ëm`¢–éo‘@…ò4~€~-0ùäÏğ]aµÇÆN½ªn$$ û²(úÊ­–ógF%†6åğÕZ?’"ID³
‰dÍÃ›c·IL2¬­·Ä0· ›G9Ü5hsŞd×ñà÷ ß—zR¸œ²<©oPD]n¼IÇ‹®øj1.˜ÒDåYÇ:•>ó
¸*ô¤tò³x>3á’3ÆZ¡U®¬„xÄM«‘´±D°>šèMŸXO3Î7N¹ıØ|•œ³„X†µ
ÆCš«Õ¼S	-ÂÿD+‚U’S}îp¥ô”¡UêPJšqj\c~¾O¼‹—y¹„ŠtÙâ_è%‹ª	ve8mÚI“GâäBÊø¥Ê))ÒÅÔQÅEÇ¤msè·J=ŸÛqv7³†J£¶&Gt	)øY:o”Â(&ÖÒûÎzç4{´4İx:ÙWYÃş*ÎÔÖ-!&R2õW[¹ğZNğÈú6­0†Êè@F¢çBgıWÓ?®èK‡r9­NšT¦$1nâ¨¤ŠË*ŠûÇ?l³|
:OŞè(pp_ ·!÷z:».Z•C´Ï‡ÉKæ”¶
FÆ¼7Êıİô!Ö” r¶ŠhK³ÍkyFÀ>	™¢Îõ¿Ë”°Áä2¤éôW_<çÇsíAÍœlÇÿö˜PL·ßÕówÕwu:jŞ¾û&]!‰êÂ+‰†ÖüÄÃ2ùì?z½¥?ãî.£œ{ñC]­¹ZkÊii•¯ÊäøéW×)wKE!¦“Óòß+Í¥²Šå÷I-Ö¢óŠ1÷ b¶Q,5gÓ¾0#Ü
#$bï5ag³¬½ÇËI¿e@âÀó¥˜x"í1¥şşNº•bz²‡¾ıÓBqfü3™d&Úr6¹¨¥U|æ´=—ê#ßæ(OÛ3û ÒeÑNIh;Ùûúìòx8'ÊqôY·]ˆøêûSÿJ¨*>Ój™Æ¢%;î1şê´ü´ÇÏ¾£>Áå?¤ºÃ)¥Ó*ÕYâË«õy˜îÓqpôÑî!ø!hÄ%l¾¢²Ôû-Ô“şúÌ=×±0QãI›{\nt_? ãmÒ»–K#RG±Õ}6Î!jy?Uøû'ÈxÔ{í±”ì}:é,+z/Ã±ÖÂ	Jg?¯Ÿ
8RÕãºƒ£¼òOœo ë­>¡ıLZ¢IP[µgN¡‚IünOÆ×§ 3i‡é¢Üè_°æM«H^ÑD¼Ûk!¥¸F754u®êT!<¬äçˆ³	ÃF"»¢øm_»M‹*l™ê…K~²ÉOÓÔ-N_N®´æYÔÔØÖÀpıØ—%Å}ü=äøëî„¿:j‰sãy3[eù„a.uuœ—®).jUzE,Şzğ@ºy’0Ö¯¼ĞM³ÑŞe¹9¶B·IA³ÙşÀŸö,JËßfajEÒz!¼T®ly>ç¹ISrçL¾ëÖäC700cNœK~kçî¤u‹³©£=t€óŞ%§öÄ0–7+ø‡f}S±(­äòÔî¤¨?ı„ä—é|aùäDHç?‹G¸XÈÏ»tÁ™¦|Áï”oùïU†fs%x£Çğ«`ê¬-?:ô@Ş×É¸_W¤ıjä ìÙİ6ìº\‹çÆOßgÿ‘Õ¥={„ÏÌ¶8s·†¸}ë1ano@r¯f°a÷ó¾Ük%¼ßÙ:ÃA¤!J¯[ßx‡ Ï¿ëı—»Biá±hàÊÙÔnÛĞ´i|oƒñ‰æİˆ„Òß2 |¦DìyÚŠöM×§å¢À“-À·çñ2O
ûC+¶á³V~v ƒmBrIß`å7rÎÑÆ‘ıá{°{õÇªî‹Ä&pÏ€kôÉo+Y¦ldé2{±õ4ß D5SáÊÎvéÜuÖá¦Á@Î
í4¸›
h¹ú`ğ3ÙMšuä“åmWC iŞ¬ A§nìêbÛp©÷¿„Âw”´—í3z™Qà|:¥Å(Éî>‹FrÁZ¥®2èV|U˜¸Y:mW˜`•Íx¢iqŞFB¬z!|ÅoëF€/l0kÀ°AÿQa+ p'š’	w‹aw×ÜåÖ¡tñq4+F•­'X°®ñÅ"İÍ›ƒ’›ïYış«³÷¾ËhUã ŞÄŞfR¿?élÎt-üˆIòL:pmóü<Tí.s6Æsı:¶VôA3Í*9Ñd¹a2*)%ÌyÎ±wòxI¬éÀ®} 9ºZÔÕ^=ıªbÒºôJ×vÌòÓ?8§j»İFßóRôÀ=pwTÓÿÂç°Só+Ä Áï…)9KWÉÃ­"ZĞpwŸ£ñ@°ŸeãÄ¤÷‡—ÂøÛ‘Íi±Ãòz>ös‰TƒEk{àUnĞyîš )…û6ù‰É„×Á!»°#Ìd\+yõº3Œ9Çv®^ÿ<_?”dÕ÷¶èµoî× /¶Ş4*WX|©=“ŸMœN6j{¾a‹pÅ€|Y\@Ğ”(¶ıĞÓe[l°l^…ø¨z~‘f VÀJ½7m7¤È W&Ç- U°"³ô‚u ¤Êf
XÖ1-¥-î>~ó+İ´¤ïë6+kb–9uˆV…õö$Ÿ®†GYÏ&.›q¹Ñ0ce.œåÁdZ"ë¼*Éö¥ÆµKÙŸ|„5»Ÿ…N£1>$TWn­	?™}cè7»×™êVê¡:9ZñÚ«ï{êôÖìıfX™4Õê,Y¬Ìıß_rm ºİİ³h')cIè7Üæ¸Âd0·˜ú_•êkÏE;ñ¥™øÍ“ş¢”~È×uÈKÃ¶º­¥2àñ‡.%û/\qXú;PÄŒÇëi5¿ŠÍÛäR}9ã‚X¾©²UX¾®ÆáÈudìß¯°+ƒjÚÚ°ïqì<‚¯m“±ô>¾nƒò‚+Ù©°î¯<Ş¥õƒ¯x$lºZœÀïûR¨ßvò	°½/]©0°»m„kÃ·–ø›“¯ÚŒÂ|m ä™Ë·+W“A{æ¯7FœiE%@qíğJÛ(W05dºÜÀıàX¡…µ4ÌÑÁòbö¨Ãú³QÒ|XG*²ué[x6|xÚx¿V6ÙEIÚeœàÌ¸¿{BªwõõP-«9&Ò×Ÿƒ¡ş½/øPuán˜ãn«Ã~ _Pì™Ğµ"në	(üŠ¡Ğìº#´-2bÏd•b4
 ŞçÛ62</‡‚Sİ–‚ß‚aåY(=°é0&%¬%&r*³¨ãüĞ4¼‘¼/à¶˜sÓËĞ£z”ÎÃŒ¦`U›î—I±B¡S£Ò: ,œ#¤ïŞ»µ…ÎhWÃBSMVH¤.ıb	¾sêØ4&Òºíf;Òæ…»ÿr"a~¥¿½·}ÓÚP¨j*±k_y’ô¿ym†7} "fÊxÚ+OĞWç}A/¿,Ô4o¡½yÇ2çÃ¥Å:¢}Ÿ›édZvØ…U¾ú¶”îzòî™¥Yù¢c¥>„ˆg… Zó*¦×1B˜)\Ÿ¹FÙÎ8ıÂèb
\ióù@[~Y càó×#>ÒEÚÙ¤Ó„‚O—ukÚWÙ
ÍáİÍÜûş€$ß^{œ`”LÇfœ±Ü`iıâkÆ´ò7ñÇ1±Ï;ğÙƒÒé@lk¯²#Öä²†È.Ôœ&¡pØÔTÿoÃ ´MhnØëĞ	Èöâæ>ç‚q§ßiÓÂ7v–ñ­XÙ|… 7œ1\àË‚+L#É
“ş,ônÊUZ¼2c-q/Ñú Œn+•ä|µÜM{úÆò€ÕQØ›ëÌ-M1µ0ÜG9ı‚¾d‡!4w‹İŠvİÕjÊ“¨ì"ä,a{ÖyøxÖlñ”—Âè½®}ığÆp×KÀGm­åÈòÙ†\c¬?Î¾»ëÉÏPv·ªÕë½(ˆÀ}•W¸[ ü±]Y^ƒ/oÈ<mX»º‚òÑr¢šı+¦³#™ëxïş†|/©Õ»F+nñ5L`»ŞT€<ªMœÙƒ˜›$Ç@³90ˆœÎÅîğ–á‚WmöU9üa³B`z3÷Èùİ$œ]«|U“4¾TP97OSŠàÂ_Ü[mÃau`h»ytåä$"lãx==íFê,îı].lä÷İ‰¸‚Å™-‹[D(Ué(ézùÃ ì'a8D D‹Bol û~cJˆ?ÇÚ\=å#v®d@Rv®9o0‹wPtK‰}lõ–™>ïqëZié‡\f.°Ş¢'ÈøÏ—sÛf±Å6ÓVè%9²İËD¿!Wog´=ï®ÙÀŸ{úr-…&şÉîI*?Aã—ğªOHš´ RÇ¥«‰\l ¤‡ÔÇ
Û™]šÔƒıFg`Ø0[ó”28k‰Iğü]ËrxŒîû†Gg táP­B4œÈ_ÒB—ÿpSOü“£rş âƒ‚d×€%Q
»[FÌNòª‚øÒ÷ND9q¾.;MµÃõ¹åöĞ˜ÓˆF@{íFÃÒAïÂh­QßÎ*3aX&_„m±Œ±Ë™ÊsÓÄ&[«»íBÍÆ”Ğí¡Ê_fş|œ=cM ;„ŞUš‚|„Ù9ZN_©;ô'_£’¸cTl„ì„PŒÎÚ½5lƒá DÂ&ëoÁÖ¢ j«³Ë,{¿kˆEdØÈØ_¤„mô.pö@ÉõVë7?+bµİeï¹v™`WÎÿÊ?.[ Ø´Ç|^é"q­p4±¯k%xM4È°óÿÕßL7·Wëˆ)¬÷R3æTŠ¢:æ4ÏßIbzÈ•šµB.]¶ìNŸõŸ~G™¼O%)˜ê<Oº\N€’æ‰l2§›À;—®±H²Ë.¸—&_m•oKğ3Ó²}è@ŞÑí Bä-¬:kº»Ì›¶ùœ	ãƒ}’4msŠ„BàÏ"-ì3ÅJp­Í´@t»Ğå-˜±eÏÌ¡Xî§´aOš,úÆ(XQ°_”P­ ŠD”Ò-*8¼Uº?FcR`,Ä˜ÊïÎGKÒ5¤¨³‚y§¼A£¤ÓŒwĞÊ.ƒÂeõ»Â°DgEœ«"²\ï•o“°×ŒtOÚ«ekèğÛÊ@Íéjt?"ÖìÆ„ürVDoPŒY–!)z}A$™Å^OòŒ#ç(´ì5e K¶$AËÃÓw8²á7ŒÎ¯OULˆ‰(8ğ^êó¾ûÊv½!Œ1Ãáeiï†^ s“µ¨rõ‰÷.T	ûø’ÌF›”ä›U¹0vHZºñnÚîïF™\×gĞ8NGE›ÓÉ½óÌL~RŠa0r-dš&ƒÀ)­´Íxïr\±µG>„;nX‹§Œpv,ÛêjZØíŒˆ¦v„Â—JĞ'Ïkõ“ğÂ¬J÷/k?‘tq|‘{áÄaõ4ıó«½æşgWì36€Q‘/vc)vC¾”—úç š%ÎöcßH	È@’ —9XË Ï|®¹Y"$k'l«ò	AÑSwÖ)”d3¯²ŞîÚ„–eÒ…ı}ßŒ°új±m)a¯¬ÓœF¬EÃ%Ö)¼Ğr¹±å|æ—ZĞ‡V/}ÒÛ
FÖŒr»›[Üd›aÀìZ?Beq¢ù£a…W‚æ„V2Q-™£[&«“é2¹MjÎ{Äç§)r4úÁŸe»Wì~ZYdbiÇ+}‘áÄ%(Rv)gBßV=™¶î¹"İeÖ’ÛbıÍÆxÅSÙ&İã:èéõ4JX=Û>0Âìí–•˜YqÕs¼ N¯˜mìKVÊœÖØÇ¤¯7B)¸O9ãFÖ°À6Ã¡óË&ó¢©„nh³Ğü	âœÅ²7lÿİvëÆ"+JÓ^ö‹:äßò˜•_ÏÓ®-aÏjo¾*µ\F0ô~×fZq2 în>?ökÄKU¸ù‰Cøm²Âó!¨†8}dÒø×1•ïßĞ\åØ«¬‹F9Ø>Q7†ãPk¹¼qÅCT‡m»t‡`6ZagY´»à>6n…îî¥o;ö48àåO¹ğZè:@¦•.C¦¬õùZD—9©f¬pCh™fnøw<MIœCº”ÓÂR½^ë@¹“¬nŞxì¢…‡¼#|®0Cæå¦B0%-¡ˆ“¼o¿ìàrlnGö7ñáƒE©¢Q‡ÜqÖL}°¦<ˆUæ˜æ`8õ‘>LXqm™²ºFï‰xª²Cv¤T‡šƒ&«QÓ[ºüé?—FGq’ğ¢W)£S8-ñóü6heˆ‚î€`á!=¤¡9Äœ9…nä2K$m«—ŒJ²¤ˆÂìhäâÑJ±%3Œ1’i\7¢*ŸÉÂ("FÅîäñHL=*¢€¾ò¾åá:}Pÿü¦%áü>ÓÊ`°‘Ü®ËnË“	-R‘½5v ƒ/t<OI-bÛä1_5Ç¦¢øMõ:zf¥úbæï§Ir§ŸÜ0wy²	eEPrıƒ¼•eGß¾•ã'Z3¬ÒP—şªyNû¦{Ò©„şEÙj›gÕ’9f²UÖ&Fâr:°Q*¡LÈ
n®3kßÊıŒ÷“—gxÉ´¾$°‚ò¿¤`ùôe ¸¦	&õmÑ2«ü5æî‡caimíÙˆ,m“kUwÚÀÏ5zv<ÙÿÄ¯Ï¡ƒ’?(sÃîs‹1í
\~}kïú4¡‹ÉÖk<Œ’¯!›EÄøè#¥Á`ğ×AT	 ª± &‰‡ƒ¯‰>B¿Á¡p²“ò•ÈFl*W–]T3…esîìa–ºÑz<‹®k9­…fXÆÄ.0çÃ¬º•$»8&«`²ì®Z?0¬U™eªjÛ»›húÚ|X³#ÑÓTvÀ&œü¸°zì ‰å8¿í0)agDq[`ªîüT·MTd+å‰i+!3¶®" Äõb8=±×&¬kKÈÙ6ŞÄ«¨=Nëf._¢v ˆœÀX“".Õ8·5Ùˆ©ÔJ'cé’_KºåùH~¦ù|í6ùp0ÊO~5°Iå¢7ƒqµ®ıp›-¤ûò“ÛşíÚ¼ùô›Kû…ô³Ÿó¦Ûòn|@]¾ßgìoÒVòwü=]?ÌŞfN²_òı&ÕõO”¢Â]ÅÌé„Ù«ÆéùõX‰ø.…Aë÷<Jüq+¢‰è÷"—¢Áôˆs®'~i‘Å7¸9s7‡	PFLØ	o%OÌ$;¤\$G¾4'(ÿå,–¸]e÷$ß¹¾6¼RCag¸¬ìzOø‡Kò¹@î¹Ÿ?ÏÙ0£P²|ÉR¤‹¦æD5‰ˆSœ,ô	ä Û5g2M~Æ#¬è7J2{½,Ò†Â×kèÏ¥_~‘®£9·|ğ—8ÖJøß”½ôG±è6°Ozë˜¿´iìÑÕİ±˜š×"~tmw¾ùoØP˜…Î¤ví¶·D.8¨ÒjC°>Ú£Û”]ó g`V¾,Ñ@ww™ÜŒ{#ìº¢#N³jrw¹)<N)rğÕW†0ßc¥”~è©‚qNü+\p“XÎ1Ö¡<¶@ZXM‡¼QcDÙµıÀÊæşîú0l+ÂæTÂ¶¾WÒ¥,5™ò÷)VGYÍ™bÊš)Ï–èó4Ğ‰CRŒÍ”»©0lû³r4s÷ö¢D¬È×´g³¯2.Ïd‘CwT~á+™~È5‹j´»=.ÖEÎÜEr¨eCï{î—Hª"E{şWy[N[s¾"ƒyMÒl¯qs»±9u@µŒpØì¹¦.6«Ëò,	©ö"'V,|ÏcsYˆÉi/·5ìï°k³ÊÎ™qOræ†Ùjål©¥’0e'êÑ¿(ô}q1Ş$Ğ£IM	7C_/¦,©]eÄó õ³4ú¸¾×¶Î7Ÿ	µ2ÜY‰é³áÇ'| M†å$6ÿ'tê%ğ»äJáš*ïW÷MTä8—şÿBõêDŒà*Ñ"­ZMtçwåmı*¨}]Goïg„*m)~¾ur€ÉkpvÑUå€–;·jDøÃ8ÂmÁ<ëÕ8+²rÕ1ŒôsNèÄùg)MÇÀ9¸÷{
»*m_êÓ)8ë=‚ã\¤a•h@J9€†G.'­[Úúy¦»ü¦B@~Öø+®¶Á¥L%…—¿µ3åş:gÔzi;°8~4]éø¥ÄÌàÊ-¢EN*Ä`mä±BÑdw²eIB÷a–v‚àôI"®è½p5°¿»"MÊ‘ƒãÉªB9Î3-—Er	˜øª:•·`ÇB~˜¬ú Ö-­ŞAŞ÷±ª›Ù(“Càà)N¹b_iN2-Ì«éÚâ›9QYOÃ<à.ØVåüdüY­'XÛİü·ä:ıÏ¥@:D†^›}i9C
Ö­ŸO"DõVpT%[mñÓæ¸Á>ÌïçÑ¸Q ˜ÕÄšŞ—Ô’Y«Õ6Hg¬œ/U–„`¢/«ûÌÀš”®ş¾ÑK¹&˜îò‘!_ùÕ TIŞ¸ˆoórp³ÃUôç´ôÜ±m¨Õy'Fşªä±5éÆN‰AââÉ$|Û²ªòøYÕºÆP­6ÜÕœ'~ñŒ$90;°€ì\)£*U‡C]Šüšâ,îùlúÕ™th¡µCÁNL÷¾1³ğ}%Ö‚‚Ó¶¬”]éÆ¨ªÖ`aQOÒ ]K¯¨Ç÷aüÄäpæM0„ŸO‘Æ³¸ı6ëØ7(Ö>>ÖyQ[täÁux°Whs(Å gò::>aVîRC|è»iØ'Ï¿“gÛæöiNOOPÅÓıïuØdS†ŒÀÜö œ$éhçá€…6lŸ—¦9B‘=üşhs—V&Ö£³«¦”7‘L%AzÂÎ D•C×ÒÁ2AJKeËtMîÌ”`*p ã>ÜI[ôs*Ëw/N„Ê­óY^#	2?¾æ¤ºå„=$KçHÌåPŒàÈ–¦jfn‡´wÖô¯U·Ãé!V_Ó"fÒ³)Y×FÕÏ¢¯fUk”™©"ù¥/¥©•?õBHc¢úÁ\œÒ:!ÜIg‡leÜuE-=-.SN¨ÑéÜ	½Y
3ÔÆ2•£a¿¾Ó×g.mÑ¡ª‰€Â	ÉøÁ\iñÿe^ÜÀ…¯eü@6s¸'ülMªÓ·ˆïıYd=ğê.í‚,#P·›¯4ÂrfbÕ+Ğâ(Á„6»•Ìqú¡Sà1õP¬·Ğs«]<¬¶ŸÖ4ûRT;’!7Ìş¼jV`YU{ôúSjRø‰;L6æÌ?îßëk°y°ïP2jDË‰¿Ü›WE i4äê@Ğ5cŒ]ÆÁeHÄ´/@$ıãQGë¼`£ …ejlòøÑi«Ë¨Òe®A¼ —ëk…±MÈ8)ÄÙËdmúÓ[»‹GJ—˜5\eŠ“%òûz¬– ¼ô"¤:ì¯´O0ît“¹â×ğ>Öïy/W¡G˜[Ò¸êÑU"OüÇJş“¶ÑŒÜğ4à¨‚¿çú¾Í7Kíh’£Ç(‹øÎ¡âlîî³ <áNW½ ‡˜Û8± â­‡-ÇÌJÁ’o–œ;0(Èd[œÓ}5ë¤³Ş8ììÿfi çı·ñ†RGÿìíÛ.¨³V)¨U$/še;¹<ø}Ê™z[+œ¿ù2âl d¸FõnĞ5áp'Ğö ‰ÁÒ!zÑ ¯UŸR:Öo>Ü:Â¥6ı¥î&˜»}@½vÿãj’4Şªä^êså8ÆQewÕ@TŒH0ú=Ş¶Wdp+ç1ùeî¢ôb©Ü2]9m
ªğÌJ¨¸¦6€îœÛ:û!Ö½{t°°†<Qdj;è€^7ñ»Ä§yv›é š\LaÁ”œÚÜ~È!ĞJùø£]ÀHæ†M?¬c»O0†ª<¬”;g=îò÷Êt¡`0•Ï½‰`¥Y·ÿ9M[Z¼Áe
*úm Õ_jİÜ*¬éÂu[Œ`/½#Õó4Ó‚Ó†P—+İ¿øOxŞ–¨ÜoÏ9Jï@Îx€dd’®i·{Éªó Êç‹0j`õüÎ n9ÃIµóÈˆ‰ƒgÕT·î&?¸†7tw¿š9²ëFq3Õ€k3¯§†nÚ7‘SïÚªÃı“óHfÈ¶	VJÊë	]ÆZïæ³tº•?Şg`éXsú¨Hìß˜‡fİ»ÍÊ'LïY›ßK³Ş»®¡çôÈìgÒPğ:P²û¨u^ƒ ¹çè•®¥È³DKÃ"±|n…‹L†»Jù=,D\Ú3¥¥K!{­„[ë…Î	ûËB`øV¾ÇÆGˆÎÙû÷½=Ò-‹ü#Şâ)O¡7'Õ³MÃù4FæÜH	ß)yĞPo·›i¶ îS¨J0„QäğÜ£GŸ|pA<i¦ìWÃ-’%· ıb„¾ö‰øÎåíÑŠ’Ëüur–xd#“â|ú×3²á‘8T5›aga¥¨tLc´–¨÷6^A}p[©:øG\¶8¸„Fe!rT
¤
g8ö÷x¾Ê<¯¶¢èdÂ •æy%Òâ:Ï©ÇÑHô! X›ÓcÔëO’\ãÙÚß2>c‰¸ÀNalQª®Ö»mÇ\ÇkÈºå÷çˆxÖµ¦—»É›Î+©Á7ruBé€©B˜MFAÔÎ…í rÂc©î{Ùœf0×pP;#¾½¡Ú[zœ»û°EŒ[ßúÓà6Ğ‹ŒçõÁ=÷SÌèX›2+é·uû~Î 9s¶mèf¹İÚlˆ:q-*w4Q¼Ş¡|&3¥{AKãN+2lï×nƒÌç+Tö+pÈvK(}LÚZ1…â–_`Ñ†Q$fmÔ¦V¿Ç¨Oã>±™]t¼s6	yŒ!i/Hp0’Î=lºÖŒPñÄëÎf­6=@µ½àbÌªH'Ï3µò‰\ĞÁLïzØÛ{q”ÈYÑ+Zõ¬›V"«·ÑÕ%{;¡.8QÄ­ğÃšù•œë~ÑQ(H­b²ñj<1Ø)ŒÁÜ¿õóùpk¿½ñBâ‰]üDñ<²R¡Ğ¢ı 4°½&ö1Ÿß‹Ä¼ç²ş#o€[ã‡¢sÒE¯WÏH@¯Íqï'xµ…*Å¬‰B[XåTˆœ?Y«yÏè,„âçí¥õ¨ê÷¾gw2ĞªS˜èºÉö<›W¤¬^8$¶ÀĞÇôÉ±ãƒÏ8B$´ôiìXUFÂÌÌaÁUÄÄ`šIsJw"¬Šass òÊsèÀ‰h;•&*‘q|‹9…–^4qØ{¯ZT¸The‘ÖÂ¹2hÇ†&‘±Ù‰_#…ÀÁ!ªæùCÛïÜıƒûƒònï"V§Æ{Ãg—©rhå¶ˆ7ën;=Ü¬³ıíƒÓ²~C¨ºHV§ğ}é²Ÿ{u[„è_’”-•Cr½8@ˆîreQRE¹‘hğÀ">ÎÊ/"´Âmp¶xÖHKr·˜58 ÿ®û…ÄA5¹Ñ}¾ ³CÜ¦°¬Çä8Oÿ7èêŠ®A’gà—òHvƒÓá
’3hÆç@e¹‰’Úe¿qD{ıf§•CIG«'uô[wH‡ßj²…¹ôKZÙ|ÙõòøG€àL•'Â½/>r.4&ğ¤AÀPÓ>²§C¢„R‚&Ñzÿˆ.¨Ê’añX*ôHfÎn9—ìª‹ïeÁ¼w:“%şÏa_	=ª`•XW¥7ï8sSXŒ>\ËvÊP1h©Q·å}N¹\ó†ê×ÛË#™ÜÿÖsö^µç/hß•°âÎ´ò÷ g;U™fi»¹o¶dí/†~ïOŠ’ü	ÇÎÙîeäI?ÇRïïô+<)y\l+jİ^ï#e@ìû.ñÛ¼N·_íÂÁgåE5o1ÃšĞÓïkŠ•–RÉ/ş¹a0+Kk`¡*wÁ:°ÜÒš‘U_Kã~ÃÂµ¼+\N!|»ŒÿÒløüûå,‘'Avóƒ?-òÄß~c»BLn˜-]¼ÉŠÙôü_ K ıˆb_iB˜
x_(cr›6à™ÔÃäş<ı À¢!$JºŒ«s eògš6º°ÏKmY`¦9%»szí>èĞ«)¡øº*.<Y‰s¬Ær’0•¤öŒJb(2ÔMQ5B2›D{AìWªZ™Áó»dA˜¥}]a¢V±ôh!J%mVÂä¤h‰µèzuHRÂşDşõmaÔ®•ögøFş¼œ'ËJ)íx¢0k³h‚á?ñõ÷3ñ^®6{ÕãõÄ!Ë@Oğ·ØHâ;¤dæêÛÓ¤ñ´ë®Ğ„'¹C ó"¹c[È‰u…Êùüøİ ö` çÒœjÎ‰o47ÕÅ"ò›k
\zéªNæç]kV}¤ôNÃšÈãÏåçÍLK¶d¦AÈ•QÔZzæpéÓÊ	,en4Ù=Á_$ÈõNÄÛ£›mšËI(ßHÌ›Ü¹ğÕ°eS±
çÏú4Îõ™ƒB'•HˆÅ¿{*K.ªÖ[¢O'ü‰² Q ƒm^ñ/:Ë‚ô³[xƒÿ‰µvN¬K€ÂÇ×Ì
N¸2M·Ó"B«éAŞòÎF°z¥YcÃjşüiî~Md`+(•ÖÓæ{›	³ÕÔÓãfF¿ú€ƒbñ¢¬gn€AM/KÂšáŸûòuE‹å™Uøì{.ÜC.³T/~‹Ø¾Å%]ÿD¹Ò¶õøÃ‰K9k;Ø Bªp3§¿aİ
cw¦dX<úûºÄ’#6yæš"j}Q*[ëZH[@3HÑØ“eÒŸ¨3ö:º¤0"8KÖ¢½ôPÔO"“vìY†`V—«JyïuHlfvœ¨ßSû/&·£3sSNKišÚ¢tÈ‘†Ø´m9¬rÄŒÒ $Ğ¢»+K0°ş«(S0!^ˆIè)æYşzÖ-LKÛnæ¬²æA.y(0À®/¼W×.K‹˜®ÂÀàPh/ac¬ıWn©nt#l\›˜é1=g«zÑV ÑŸŠğ<Úx°lì
f©ÛÎR%êsd˜¾©~“öUñ[©l3a]lôe¸fx¡w/\Õ¦âØ¡†&“¶HCKÑY*¯ÿ‹ÌÒ¸¡ÑƒzÆ”d >”S†Í&fMò‡â!Ó"åºíıG\yå% 6J¢ãÎ÷®)^­¨J^‘Óüh[Ø
2¯ªñ4)Õ­ù_§°¥~–O¿’ÅtŒ¯8¢æT«ØºBZ'7¯J¤Á”–¦ëzlÍ=7ÿc‚
í;+Cå÷§·kßFz°Á¥ú7A­±¨2Ñ>’j©¸Üï?ç]ù©|1›'½`C%	} ÎY˜ ò“Õ•èÑum¿ª*ò†'éè´3>üÍ_z,¦XsµRù13ö>*udÓ§~u>s†H‹İ»µ‘@Nh»ıé1Ï÷®B”É(4ÓVL-,ZÈ”UÂˆUÖrà¿¥ñe“«ÀMBl'Ö/.İ 7üÄÙöô HRà
9•„–iêXt»'Å”la@ÜÖÕë³§XïúGó	÷=fí‰ğœ†ÑTÃi;µè=­àµ`é„!©âÑï\îÔŒì”HDSã_°!¤FzÂXD’?HĞ-ª}°71Œü¯ãÌı—@ˆägºã¯i†Y†ß1ˆ¯õ×"àgŠ*ÇÀÑĞş‹9ï€=¨¿aš¶gÙ¥´¨ ˆù§Òz
G©İBl:¡ñ/	³¡$û~q­å9Ö^uê+™5Ó°`uN Ä)¶-¨,‘6Ø’ßu/’©MUXR±›ŸØ8¿ÓÂ…ŒGçÆ#”XBÀ“ÈÚf%$â†Ñ¤Ã¦¥ğqp¬ÌŠ¤`²4ëÉÈ ÿYaH¥m–âh:¹Ò% v(	²ş^´ñJ$%wæHˆŠ'IÀÏÜacµ"'Œ¢=¼^WtïÉ:®4æv‰Úõ¸+%ZÕ/$;œq M‚ù(äv±ô6ÅÓw<íVVlÁ!úş¿[VÿÛ®:àó§x1­.gä½ÿì¬•±åùÊè=Ë›pce#ušÄ(:õåTÚ®?_RQIÔ&AEøcosñàBÈàÂÈàëƒ$5|4şH^˜Ênl~¿Ï¯{dx‘À#ùrækáv1²á³à1ÛÖŠ×·6ãI»5”^Ee£y`$<0ii§’–³ÿø™ú	"%}iéîñ3}×éï­£˜vX7>Xë¯@A»0Ë¶å…ÅUE ?Ö$JQiè­¦«a9mÄ¢Ã×Ø$³ÈĞd—¼0(üœ.ºZäÃÂWWëÿ†&ºnR4õ¾ªoAP»ƒQ±yª¼»nÀ—ÒŸ„6µÑ,Ÿ °svM¸V‡;½ˆÕ?@nüV8ÌwAguñ_34To¦Pd‰¹ÓyZñÊ·‚…8æ¹Ô¾T‘mÏÿYˆ±q4:Vúä…ıñºKï÷®YUËz^e$Mó~·X¾Ï»Mä\¹’„Au%"âÑ~ÖD~œ©+	Ë?:;Eººî‘hş7'ªgìÂÙĞ}˜,ÎÜ8 hLúÁó8åØ¯<{»{6µÀ›õŸP3İŠám!zMíŸ™œ"À3)@ÀÜÓxE•˜	³$¦Ğ¥jqxğ%x@“!Qe]Ø4"âLá§§‰fI™š2'QÄ°"°æB#§«gG7Ãù ÌoP›&CHKF^)ƒh¼ÂıX
×QFdı>ĞìF‰Wz%Íİ<–Qİ$Ğ0ˆòÑiZ*f[oˆx²>…rÀË™¯…˜¢"f ’Ölv>_« ²;$†¡ú[$1’kuSLfr7§*ÂÕ•|<Fd”¡|„!/°tA7äU¡tmê=7¸¿ `IÏ™4òBå©© åˆËWêáæßlÅ§5luÏ{í%'j½$U\S‡
èÀdú |Çğ=J‘TG*AJH*¢TKº¬ÂÇçÏ2‘P`Œo$1˜¼jÏ”æŒ{æ-^íSÉpèXlC"e:—ë¬(#ç#wLW¤ØÏdTÂGWgà’İc@¢Ø~nä×3ºÀö¸G–_ŞÿúIm³£Rhr6ùíæUJ‰÷ûaMÓ± VøıÈşoià„ê%oD%Ç:[¢{{RKÆ+…Zt=azïÉäQ’>nîË Ê:\î‘¡ÀMèIÏm7á¹Û6Šö6Ê‡¨$¯a„ó6Ê_Ş"	¬ìqGÉò‡g¥çnßîÕç»ıóÍ­§ß„Ùn½z ızî½³ŒA¾ü«c0c	şİ^ÒxÏ«Â‹öOmİ‹®Œ
>µv64q¿Ù;*ü$i¿Y¬·“UªŠ†Aş}ßoØD”|ÁPî‹Åfù/Ê-s×•ª`al9§a9à^¼¡KâòÃRu£yö_qY½Ì°íqıG³°¿Oœª¬~šşšTú¶çïÍßÄğÉÚp7~í„1£kùmõş>v·CåxŸ?Jş#7‰ù—åäİr½rƒ´Ó’ÕÒK²Làªè¯qYSB'Î¸© á|S¥êÇ8Ş:}é–?õDPó’A'Â+¦M=¢g¡„t§Ü¹„Bš¢òO›ÊÊ¿Ö›Nî>ÌA­ë•Ğ[vT‚ßİú"VüRºÚõ#dÎ-$Ç­IØY4×r1'¦™>=uË´ææ…š×¸.„’œ²yÃÉ•=ê"º)‘í“\£Bg 1<ì"}Uj”eÇğü¯4@ë.ÎÜÇuPJT¾;5%Wâ°şµNa¸\ù¼¡³íëq»l1V™6‰Ş YÖds¨ËTŒìÛ,0nÔëNMvs’^yË3 éWœÎ#uà¨ pE_¨)¸tZq‰m–ôòNwF|ˆfº‚KÙøŒ Ö‹§~ûšqbEJ¦bèãê¢ŠaO«òr¦`£sˆTª½­‡îíWçÎ /¶ÅlÖÌÀ³$•z®g³ÀıåRJ¢ä§¡%ñ€JHŠ”ƒÔtäÂê„>zŒ ³v³ÎåéA"/~İ¸´¸zœ1<ÿs†02Õµ{ƒ°‡­6ÁK¨åjÙ®›Ê«ŞN°b™	a6¦sq•‘ÜaÏ>@à—O~WJ¬ğalØ8o
â‚ÃDÂ9t°fI4Ô·èøÚ6—D/(n.>—‡y¸L8!®œÙ×,›ÉÑMï&ùDÚ&‚•î+5~xôˆî…Èl	!å«½Ë]×upD®ï«{RÕ”~Êü@4ª~é°øµ¢RtQº#¢œz	Ö\Ÿ:^_¡sí¼•ùXû‚“¶Ü±ÄzÂáËÜy‘‚&Ú UãDt%Í:Jò\¨ü]Ys&—ªª~l«8+wÕ9 ´õ¦8-Eëƒ¤“zÉªX.$ã õ¸x2°>÷ÀXİ×0ŠŸ6ù“¢â¸9(H¡g:‘¿ß¬Ú˜_ÄÕ>Ş–í…úIJ ³M…õÜÖÀC$}oaÑd³‡ó
³võ®mU0wósqûlx[–-’BGvæü‡üÿ'@Ø¿ò«mcñ [ÛXtœÀ§›5nVkæ›=Íf÷?<ŞÉV®%ÌúáÃäĞNfHqBZoMY¨ººM;B[İQà(;C‰ßŠßBö¢“©úç°tSëã†¨pİ;ôšW/TNôi5©×–ÎDaúRš²Â>Ró
cß`Ñ3ƒmCÊÏ×ûÕmuF]owim!5Œ¾¾ÁSàoÕŠ@o!§é’TñMğ8¤ït¨ïQr›Ği”Ì7ä_êv¤Í\PcïÒ†ù[¡+Ìà8ø´}Ó8U `ı0Ä«ZH>Û„$SÒNÁZï‘J—V¾+KÙ=âõ„›)¿mğòHµâ˜RX';¼İ†sšëIÛ*J¶)Ä£`@Şd—&›’•SDŸ
éF¨¯Õç²@eÈ6ù=^Åéó|¤q>Ñ?ó-fá^ÀbxX —Š	Ì¿/ã'4ß·sjĞgu,£V<) ÚXÆ-Ì¬V5¨âu:›³²ñ¦¨é‹h›¬nJ…÷JØI%mi?M±Ëf“00ºWòA`< Uà—Œ­,Je‹.‡`†ï=Ö€ö¸HoÃqÅA<¤·»Ôÿ‚š!+@r©ô\d`òö÷¬<|sIÉ!™á§ù?ì‘jkÖÉ˜n³k5"ÒÛOö¼{06CX€1ÃüËfIŠ¿…T“Úc®€1ÃüaĞ&«„0]•BjèŠT Vcú•Xö˜hwãuÿo–= “”æûƒÉpÑj®r*` ˜b`r¡*pÌpÿ†·şCib GêÇ!ıË2:7­HƒÔfï“° ‘Ş"e„~õí|ò—MYswÑA¿‡Òa“÷®qU˜:iš÷uéß<0ÉÉœOâQ€·ñÛxÁ›õÕ‡Œ%pwe4õ“0j;_=ï:yàK
¨[ádõ¢ã]r­12y6n]òöpÛ‚¡‡›R.ZK÷±0Q#ñö§“ñĞ_(bwª§½·è_{´}îZîjæ@Á]T(âxê^®Œ·Y:"³kœaM½ê´É‰Êì,‡™åo÷êê!äN#È¤˜¡ÕdÉ.ı·îQª3ÕSÁ>ÑÛĞbÁ T–şË|yX]¡Æ¡iâ%kÁ7EĞDåÁ
’íPxe\œ8;”XÅ^C…¸ÑåoÕ§èf×	‡ÃÈFCÍ®C‘§ÕM¥ÔúÔÕÚša~Ü•o¶<&G8šx4G‹Hõ ’Yh•`Éºå¹ÏÑœ²-›ÁzZwº£“ÒĞŸ<UÊ*6·x™Ñ„‚ê4YÏı•2’‘å¨
–1É÷¹Cˆ×ûo[¬¼[•˜‚xEP	ÀÀÓ2=U=O^Rj>Œq(yWÓ´äÇ4ëQ¸Y³Àóú×².+Ší,¿÷úF¶´b€-ìlÂE››‰ŸLfã^/R< cšp¿]Ö‰YÌ/‡ Òƒ:ì:~ì®;5<v¿ôB®;•ö›J;–QgµnNmEÏíÁW-Iö_˜j­V#i¤Ogã±TÏü»õÀ°qàfìÓéğÚë÷%>(MÂ_i)"Sô@ßÉv‹‚tî¯w_¡ˆ»d¹'jE–ËOS?xDá|Eş[×2‡¦i)½Yz»Nãôùªì‡È/b]^·6U2;¸=—Qo$n6Lyvåg$˜ÒƒX›è•ªä›hvxôMTãc22]ø¼ğ~S”‰®eÉFØR]UG`~—M	âà®òã?²¼½ ÃKQ Õ0Ì¬¢šzÍÍ*/ò«\Ğ
­­u·V‹±k;ŸcÅzÕ>³’rkkÎ~Im“…¹} ØÑù„0èÁo²é¾Îf»áNGí‘kæ€cfÄ:Ç¡Ô‚\ºñ»ˆ¤Z%m'¿ÁWÑO*„¼‹¿DØpzÅ†™ä_ü—@·jö2œ´(MÖë½^üùÂ›šeÕq*«zR9u5:óRÅ/_=,ãIÙìä/±TæjĞ“Ÿ|úßş?åënÿw6oß½ò¦ÿÉ ªÂÇªB@g¬v’Dm K¨ M¬ş"º/G%l\‰iğüíÛCÚÏ¥ëĞŒtğHæçKÛv/mS™I–ëª©­Ó‹	¦+o!9ÚÃép2
W]v¦‹ágåk>M‡¡d8-Q~Æ#­¦ó‘O[%·Ì¯ZŸ‡ÕÂİêB‡¾Í8Âî(h«è²§írXídIç„ÍMb&û-i~7W5²jsBÍş4Ó÷ôÅ–=ô‡a?è·mÿ¶~“N~èË~éÈûƒúä9 `áı,1€E'PÑØ—Ö_ €8ÆÃÍØÜ¼L`fğ|€Í’iK¡ä*8"†<…tt7éGşÔS„}¾°¹–e“ĞX_=ô´©%`®G–’ãX‘ñY–cVµ)4¾½@ˆŞe.V†X·ÜLöZµ‚2k5¢VÊŸ€İ>áN‚ÙpØëšR­äú1=Ø™4<ĞA{=h‹Îş—8h«×MgÔ)Õ•T$G‚©|n¤_€”V©¥Œ–/ï™w<Ğpò2ïûZrØtyK^Vµ! «wÉ)E@¿‘fÚ	7L´‹4PœŒ
'tÔ:{JõÖAgÒAMã6BC­8dqñ Ò¥Ÿx™Ë'Äqôúm´XD¨¯!u~Ğî»€.oßJ <l•±äHÜÄƒşb:Unâ×—ğ©šutéo&6¹Úr·ò—Ë-*~çµ4VC×—4rCµóÁğB¢Eº¼¤{—2§aAb8ì&.ç{;¸Ÿæ)Á­u¯ßUüô‰ é3cy»Ó=4r8Sâ®Á\h»ñÛ‰ÿL\>][ÿ< “%F:ZS¶tÂg¼=Ñ-ğ5˜M?u¦‡—ê´9³	ò›«läO3UAú9ºqŠ‚Ñ(
Å!Z³'Åÿ8z6‚”T…`D´ğR»# È@İ?Û–3[j¢m‹=.:¾?÷£@qYÙæ6ÒªÔuPíwL»V°.7Šïíğúç¹'J—sû)–Zö6¼ÏÑ'úsîs?çøD­›åÀQ*c_“G€Íçu OAÛAƒÕİš]®x¸N\~o<ÔÕ)>¥÷íè°“T§¸ÖÍ«~İe×Pw(½!÷F(4*ş•÷¹ •Z…ÏrE%òñÉ4	E+ÃêiV½Lx›ÙÏelå“ÕJy:g€úF½ß‡^o‚Fı….>÷2+Å]AÈ'üû˜Q£JºO~x¼_äOsßGá¡ó;<b†˜Ç<e¡Ãx½°?z¨vˆ'“Eö(âŸ’FhÇ^¿çÂ-©òÕru…H2İ €dögİ¯ÜBÛÈ¾İç+©íC5-™[ìrC.ô¤A@Õi·‘q\ª¾®.eŠ¼ıˆ¿&[“]íğ“ì B‹Á,Ğø‡B6-ª:‹å—¹P¯.¾0ºŒ|Be][aTG)n+àä+ÜV?AÊÜV{šÖºñu³à-'	§[)òâg÷ÒıÔÁ•.9Ö8>XêÂğ'¬w•ÂŸ,•--4Bn@ğ†³ñƒ/ö›’a·¸±ñİ^>ûçİ¬:`ÊqÓÆ­×Ba±:5µrûùD%ÿDäÈZë9ëü&Ïui¶TuÄU¡V÷U0Øş^XIPH¥ßWBG¿|;IµÍ±Ü8›«ó¶pÌm!Ÿ¤ãm#m(–ÜrÂë0Öãúzí„$Úø½¹Súí:Ø>oR²ÄV§wzßúë0Y©àTü}åáœ|ıIX.;ò—ZXĞHŞæÑg0Ús»4œ6í2gêÁp2ô½±™ó	tà>iŠT“Ÿz¿)
ûÓ ßŸÚ¡vë6Üª×TÃÅbAH³×Âù´¶¬…‹µ	q‰°%¹m$+Ò®îjã¢;ñ³¬=xsÉøy°abM£’A”{eÚ¥0çŠâ,ÌfÂL~Ø›÷Ê†öÍØ1]^,9óİO™`—Wô
ÛIi<),#ú{?ğrT—q»c#“mVYk‰Tµi‰¸œY@Q´3Ãó¸J-bgşÌB4ñã™ŞoìNÇı ˜ú=+¤wD"~Âa‡{0ô†¡¹' ¾^Ç+¼"Ã·«äcKss<‰FÓ°ÿK+W‰İ_ôûüåš5]4;¾Óéb¸è†/øo·hïWïã(İYJÅ,ìE#d7-TŞ ÈÇî°NÚï%^à…^Ğ€Š÷hU]	*Ö_¶txA?š";­À³ã>ÙÄÁ‡À’ì›G½FKã¥¸¶âï“5ÑÆ”æ€Ü>…½­ÕÃgÍ®¾YàöZ¸É+•“¨õ%O¯CNôƒh<éûºÄ^]åDÍ’tÈo6ñ¦£‘.•d§™ÓWò-ñÄ¸"× nÄ…Ü^;L›Ø6'³A‘q±)=êÁ"ü`†}_«\ÌçKN•‘Šo6ZRùQ÷ÄË‡7y»[Ö0/"ç1íÖ%-“QyÈ«†›pãl&…+Â¡dä¾‘¶o®¿Ú)ï(å(šMdÅ±ãÒ†ÊœM'¦éÔ—wææ8E¶H~—la^—®ó%Èd×á*^LŞ>‘ğŸwñ2E[!ßïM|¤H7 ÏÆ”ë»ÿMb¹¶tşÿñ¤Å`_ãõS¬ŠÔ`Ÿ¾ğ~]÷õ‰š®µŸ7ÛÍzªBTû9^G‰öã¥)V¢zfnb<ÍòÕM±·„±üB³$l‰j{Ø&ûÍ<ğÇ«ó¡§n‡åLû1J¶ûUMßıbIjíèæ[ı¥îÍLİJ8Êš:ø×45í OkjÀ˜¾³©¥¤›nÀğ‰2Ì‚Hu’ék}º"Ó“`¤i)&Ê§ÚKÓVÌ‘O”•¦©˜ ?ÓlšÆc1è8ÓsšÖ1vÕ4óî¨i:“C©FÔÍ‡8y™‚Ôµ§h][qö2õ©k-Î`©Mu=ÄydÊU×\œK¢kumÅ©ÌT¯®µ8•¥&Öõç³PÌº³Ydô$ŠĞ£™é@—Á%›k@Ÿá%Ñ€Şe¦D ]F—T— zŒ/™Jô™\æšĞizÉ Ïì’êÈ\’Ú<Lß@ºaÈÔ¤–²l ]†—L	A:aA(u¤ã(S/Æ—T3AºL.™‚‚tÂQê)HÇÙe©®{‰¹_Ìã²~Áú,»28éQ1­"´‰·5V÷ây^ŠÅÒ¦ùË¡q€5&½adv¿/ğÿEËxm”SÂùèïÒßúa8#†İ²L,„j€ÙÆ©õF=Ï0 ¥Än'áS²N›2‚qG³	ªB³}H±˜gğ)úyíd}F>I.Ÿ›°"‡áŒ'$kLŞq¢uš2nãJ­Şjè‘8ˆbËäv¸Ó2ˆ&áØ[´QU¸‡gÙ¶è†µÙÎÜv|Ef÷˜zy^ú\)·e°²|Ÿv3g#¯ï÷ÃJu¢ã$Cûê¨Akà¬{é?àI ÃpyÄ¨‘b#â£o’›É"êÍ¢à(RÓ‰™…M@5§Ñeöt×¯fª›ñGÓÙl2™œÔ7¦ ªG½†ÊdÉBEMĞ"˜xƒ³Ò˜~kw–_~¤<Û~õm›Ú~ù	ÓÍ¬MıÙ$¬D=Uë©«VÉ	¡Ö¬…$Y,öÁ8òQÿ¬XùXeù
vó¢aeÙı´ƒ(«ñtÚ[Ì<EÕ–[“6H¨Lwvóè“4ìøOh25¢f®.ˆô&Q+#oÔ|9JÌ5m£l‰³Q0°Ñ`ïflrÁËÓDZL2kRÂ6N3—Ëk<"Ø§’^.ã·Aò£O¤M˜lÚ˜rĞV(ø‚Â°’H¶ÏV›d›Ñq¡ı¢I¢}í…;Dñv5O12#õ¤¥ö¸ªJK†RméZ[©00°:ÓSÕMÕÆËÊ)?²àÇa»hÙ?Ş†qJ	u°VãÅ(¨ho>gOùn´‚EÒ»pEìÊõ*4²Z¦ğê”hèæ¢Ÿ™¨œş)‘¤Å0$–[èä_I|®Az(º;UŒ”¥ÎÔ¬Q[›ëRÄå,êÈ¦Ñ&¾\UÉ Õn6KËDH‹qÂ‘a"µei$/ıQÏ³BIgôg:=nã„Ém5ø@3ƒÅR¾-æU—R=Ë>{u‘l²—}ÇO`N–<;ò¹<ê•’ge²¼…‹Ôˆ/­ô	#mo2GÿîB^BÉw(ñ¹"“E–eYN4ÆŞéHtUçİüV%˜‡[r!13lš-·9”ß|W<Mı#ˆšµü`RRlïæ1õV ğ†ıÂÅx2	{ı`¨¨IX)|" ¾ºÈtÖcP›d³_úÛûÿ¼|Mø1!Õê°I¶}.u¼ıÔçá®}åÙ®®èÑ®ˆñ“]=İ<äs^_hFñ€,øõxªÇ´6d,d4h¿¥í¥B>—¦í…9<Ğ­Âçê¼´yëbÉCö<şnÿğ€v©QmƒÍcây¡çË*ò‡’äOÎv‚…%ØÆ]©êAEÍªQó*VÙ¢^• LU«{7Õj&˜m¼ˆ8µ%Ky:™“Œ)6V%7h rˆi¨İë$½£>ÇÛO.Ã0ôÆƒ™â½[ôÙÕ§ú†¾Mç"ku
A&ü(Ê@FË+é+D	HM€k7?è3õBNf1+–§pë©iÅV·ˆÿö#B›Ã—(¿lè‰Úx%ßü|{ó=ëm“ÕM9ºWÂb‘X¸« a~É÷Å¿r×G²\fóÃï	é"È_ÖƒS9ùPò–2ùÁûª#r*0-h"ÃqÈÄÕÃ¢%îï?6%¢
B‰®jŠÀ€à|CsW0ÆeÜ¯Ã^…šš«ƒ¥0wE(„ª÷$IÇ¿\â+‘xã¯ÍÊÔQ¥
¡ê3ŞŞ!ã@!K¤Ø˜(LMEé*"€÷Ï¬ì›’¤ 	åR?Şûİï¡Ö‹äw¼RC†ÒÇôŞ¥‘U¡æ'ôµºNšR#„PóZü£¯Ÿ©mwGŠº6¤FÒ‘ô—)“Æ’ƒİ£TÒ¶;”.Xºâûº}LÖe£ da'|xãÈJPå2Ğ&îVk ĞR 	İèZ	 „š;gÚx^æ<pãEÁ‚Pğ.Y†X˜‘ƒ-˜ÔÜîT¶…®Æ½î‚Gî—\=ŞÆ„ˆ É‚S$‚é®ä	ñu‹î“Mc-&ƒ„êò<‰LseA‚â§‰ÂİÁ³Ğ¥lV‰•Ğ˜H«SÇM‚ÁÄ÷ãìÌQ‚„Pô:Io’ÕÊ_‡yÃñ%ã'ÊËæFXH³$i=»²ÃªĞ,d–;@:Ybã³–s;Iõ­pƒË.œ-PÛ“<S_îóÀ 3}§öOŒ?ä™4ÕX¸R²4ı'æ=ïıæ‹_<ß:˜dï²]IiXƒÕ|6ŞdˆPzœÆR»“ØÇä!^ß'_Ì’ÖÑPB²ğ:0XJHV–“3{É
{–üaĞ;#£€ha%Ü'	‰Êqd!0hP[ßÍÊ0[Ä£]u7¼c¿RP´ íSd&_ø.–ˆ$HWøë‡[?0J¥\«)É¼~òã¥¿X:»ÒÁm´·¬âÆS¥‚iÁ/"{ùHŒ$rpUäŸŞÆO~`”U¸Ö5ñ†<»n®İ(ø­À/kªñÉß6^G"<-[?reªó°€^·ë}úˆ5ã¿¨7êgpå>áAw?RûÒÅŞGàXÜ™8;@‰ğ@‚\ø¸°Í
@ğÕ*Wò²_©êr^µ@hûz™ä.®ğ(x”…Ûğ
«h|
ïâ‡µ¿ÄöÜ!C#Á¨@…Ğu¿õ×$Êt®÷aœ4%H Ÿ©LÒM÷z`F¦˜³\94Ï,î»ùËôñS‚Õ©H;I	‹J—¬>&øàF¯0Pp?;ç%Í2¦ÆY¤ÏvÇùyºİv(@ğJ~Gn1´Ñé Ïh‡š_UğÀ,NQNL˜¹m«ĞàÑâİº£°O	¬ı-ì»dûÕß6¿lTC…sÌq\ª(Ì›Dô#ÖJ¹%ŞÜ—$ ´ˆKo‚ (vˆ!8¯S¬?¡õ‹CšíÓ\*y`ğİŸ¿h¾B Áö"Ø¥†³ÄÖ!¶oÿ'‰×ŸÑ¿öh×\8 Ady8šKGzƒì6¬X†U+b¦\D©À@g‡GÔÜDØøû]	à¬Ş
ä[1³½àZÆ’:1×Ex–ñ4®_,X½Sp}éĞô¶A¸Oqa'¦¶¯qHß7(ò÷Ëô5ápø9~xtC’leb‹ °-AMŠ}šÂíÃPp/Ö;RHø>ÙÄ#O°	5®‰±8ïD
àzçøTêSúÍ_.7ş¦¹*¬ ƒÅM¯:ˆŸ’[?ßæO¡›O«á‚=*.ŸI á²Íp´Ôxˆzş¾Ç»ßg´YZÄ\ÔÁZ{E×Æ7Ó`p‚;BDxV³òìÈ— ZÅK9"F„Ÿ%‚#OÖŠK®ß´h7‘n·Œ“á‚O{d"<ØéÂßî^ûK48˜†Ïè	›rÈÍ³"ğUÉí~<âÏ!Ù†¤£Pí¤¸S>Ã®¢šd@>İíEö£·´Š>É@¡>èìÆİâ6¶˜İ[©G?^Ów	8˜àsna:;¿W!ZÓãîU ª‰ŞÇQz½§k‚¨'ZQ„Ù€¦üY£kº
¸ q„¿÷WèÃ:j|ËRfÃ¡!øÎß¤J ø€>ûëÆ!˜
 Óª¿Æ&Jˆ5kãói	t+ß$«M²C÷.¢uªĞ t»
ÂJ–tã
âóKønêõ6ùºs—KC†hG3×†$ôíü†\-l}‡™`Dğ†ÁÑC½İrÍ,LMy_¯ŒM˜…|ùûÁ/C«¨3Çu=©$PYƒFÎoÈ²øıBi¹h3&CQ¤¼srĞ„‘ÇÑÅ«¼[VòèâOº¸Şl–ìyÎÇÄ'K¾ø›"9Ì‘ì™¤›*¹`A Ş÷|<C÷³!ÕPnPı'ƒÑjÑŸ-
Pü„Âì]<~'å*‡So1™(ê£“ÖqøWÕ¯Ye<2-¹GÉĞªFÍgiU¶àuÀ¡L­J âZWJŠ{73¶Æ!¤Î¨0^—ò´fmc1ï»ÜÄ¾°†Â›ø.^š¬Şe²~P­Aú{?°ğÌıİ£r‘b—Êå+É—/èy×ËÚn¶øÿR›»,ó*eSw{}*fÅ¸Ú\æ
tÎ
éüşd1B#İÖ\--K”¨v{òS_ĞnıÉ0˜Úb}?ü²Y’½My)v˜€şt8	†cÃŠz2oW›TiéF=öaoØ3,](£¶¸Áv.ËX«[»<¬B3(¤¾Ãçbİrm(ÅV1ö´[§¼L»ulÃÀTÚ0pnœÅ‚[én%$WA’º}A¬aW;Q;Ä@¬08Êœƒ­4@ªéİŠ^¾ùhDï(³`ÇŒv–b¾#öRä-GPÁ‡ó–ÊBğ¼lgÅ'•¢ûõr™?448«°Cƒò8RVÔ+#¯H‘qh±¾{òB ñÕWX21_€0Ş$Á~…Ö˜€ıVøOÉŸ6Ï<J„ÎN=A¸Xh±0³ÁZü’;ÛMÇãŞ Úãı)I?%aÅ® *†£¤wetÅ’SN?|gÉçY–kêM ëvĞò®Cq@6¦!jj–úä$*Êå‰ĞJ[şğÃhqÇµ”å«ºëö\1Ü—ˆØ…?}1¼hä)PÙÅx´WO;l#×½WşóˆoÕl;Š__š÷ë+[@üúJ ¦~}önúõs«/9ËÄ¸àl.‘5 ‰èÃûC¥óaÚ˜rØ´1 b¬pxƒª¡Ï×A¥h½^G’E¦ËùaN„ê+š³³FG±ÂŞş:LVó84SqÔÁóşèãĞ\worğö¯:ä,:Uj¸xNÑ?şyñ0_(‡˜ÕóÛ¡Í×‰²Æ£û\€V×e³¥™6u±¥³£ˆßŸ‡>
Ì¡’l	²ÿk*Ì‘Ÿú“I4ìyÓéÔğd¢&#»İıH¤Üòjd4î{˜&D.9w˜Po13<ªé EâôGdyK†½
ÆH¸^$[{
Æ“şy£F‚ùn¹ß=²„IvTÁb0{ı&Tè3p!BxšÍC@t‹õD`ÛÚë-P
ø÷ÕvDŒúø§·è5£b‡Õ¿­~
fã~¿Ñ<°|Ö1õƒ~äMïÊ5ª:Ø"´Ş=&i³ùğ§ƒ(ÇõµºF»I½Y¶êF2ØóM³—Ã¼ù¦İ²ïˆ3(OôV ·åádj9+ÃıÄæfPËKO§;¥”|'“YµÖÜÏgnQ¶<Ÿ•3›|h§	qr`]œbñ$]!¸œ“)/c÷³Ûí-Ï6wÚ†8öİóL7şƒm›…cÔŸkÜP~êê ŠØÏ*Vla#`§Ù®ßÑÔ`İs%?]·,ï™/c”ÜŒßWÏÅŸŞ.êæÊJÈøh'j/w#´°:˜—£{zü<ÌÕæ¯ŸSD½âç½6k[2óêEödŞÃÔ‚Ê\`g+¾3V|æËs?Ó¹Ÿ±å©æï© AåõÕéŞù±l\Ø‘Øä.Ø6|7™‹¸«›˜ÙÄ:&6V;‘ïÆ•+yÌİ‹NîÙ?o0Ù`ÈåD{vmÒÑc êıì‰ƒ¿ÅÿıšlÃ»Ï·ª`Ôı[ª*at,1›N{Ñxy7Z!¦Á³Íp0‰¢Şt`‹ó¡…›"ŠömÊü¬>’&¾HóYîè"­á¿<]q#<Mûn»QE}±`ªkİ‡Omû|2º Ò5ôÿ©ÚUÆ"#cêÚ¨Õ§>P‚IT%->ü[œ>~X?ahNÂ£
P4Pl³Y‚R%µ4ıJFÆ<HöŠ@EÒb‹’/(œ-5Tê†y” n%f>~[Õ ¾­êo½­ÆİÍàí\d 
¥"S¾RÀNôÀOŞã3Ş<ìWÆ{°µ’K@äïÓÇùÿ  ÿÿì]é“Û¸•ÿW´ıe<UHİS›Nâ+ñîÌÄ5É|H¥X<@5cJT(Êvo*ÿû @‘8H€ÕT7U©‰mx8Şû½ÀCœl· €Óazü™k¯g‹PœÛ‰$qb¾ œ€¼OÚ¾S˜_Mše©~ÒAjMUì`é5´’3¡JÙvïßÁzyŞ£ÛÉ»şôggú­È+áF×‚©Ëp÷ØaB6í“şôˆnÒjæ¤43µ2~(0MÂ(ø½@¹[Á‘:Ÿ£ ğW°s@”DVJƒ€ÍE1Š¼Ôqç–=_yA/ÚÊà	8: àf÷§øúEà}2H	„®À«(Û¹‡7©ûÅõbPÊÿY
‚Ü¤÷š…‘%f,¦°Ü3×³ó1˜~8ø,œe/ÛŞ4V‹`­xÈ^BÜZ´#>-ËsâÙz–x—XÊb±ƒVãˆG½Yµ£½ÙLk¾Q<8ÍÑNâ–Ø}¹XNW«nlÖrĞk;°Â•jÎ1–¸Ù¸+É½†¬¤âZÒ\¡6(ˆôTÑ¶g
+‰wµ<ÕiZ™rÎV+ë4S§{ÎT zì‘VŸ×âç¼ 2~YoÏ+ßÊĞãHÊòÅW9ıTYIÕ
|—ñB‚‚•a( @?BÁ5CÌÓ±dNÉ4×õÑ/Kí–ÛÎ­q¢¥«û‹÷;wHöï-È>’*˜…ó¢/ª]¼Üx7ß
úñh2`ãù—û4Éİ 1úås¿}	^ QK5†Á´’R´ÚL/pï«|&õíÅ3d×­…a\§®ZÏ¸ş¼a÷ª€aP²Û0U7ø¹ Œ¾Ñ˜ÃĞ÷öeAÃ B#!#¨ôDcÄˆ.LŸÄa†§á·‹0üÈ¼İ˜wˆ<iRÑ°èÕ»êdcGRœMµÙF\Òîà+2\{i‡%ñü%yŞ€C
à_ù4ºe¦ÿœÄ§9éÿ;~¿-Cq^ú÷9V´ì’*å¡Âåşøß”¸œ®(³N5Ï=ŸøÄcƒnËpC]5û¶€ˆ~ƒ×@`›o÷YTòxŠóÑîƒ<oú/?üœÊW§(FìL×±â-ùö=³—¾|SØ ˜O9ıÔ|ÁOâØ=ø¤d†pµ…Ÿb°ßf|Núy¾úSÊŸ Ÿbw¿=ÁyîúWç©Ï-áîa…<İ:¬{’±åa$ô*{ÄƒfÉ1Cã„_ë8Âjé.ƒ™-İ'.¿É¹+wëÈ³¿¦ò'“§_—>×®¯ß4zâ3Ùí\qò¶â]+0_-|é	iëowu/=Ã™mì¹¤©ç¤¨yÀ~úÕó6¶çn¤ÙÜ¤Íşºÿ´O¾È_Ñ…S‚©½˜IÏ7È›®]¿µ½×ŞR"ŞgPxüš–Cw:_¬6vË?BDŒj^~Ù¾»YNCí¦?¨¿…™DiÓ;ğ]ùm ¦^ÿäBK*Ï¬:öt–k£Bß	ùQB¬†ÆÜŸƒÕ|¦?M¯İã}æ
³[RF–ÖÔSë>#öâÃ.EjÌåláMõ™æ/]¶×®=]¬¤'“ä³‘µ2N—›Y M0+gF¨?Õ>¼\9hyú×)É´ß–4Ó‚2áìõÔPŠö”ûòëHq´ï6	E+êsàî?½vÓ ÛÀI#ÊT¡3Óq¤¸ez%\éDUŸ81;³d÷v—ü3êD»Ô2íwIºs³›—İF^iI™ş›(ßï¡Ešu"~nF‹òÏà»~·q—ÚÑ¢'kìL:o†¥,<9X=Û«cŸ#7¬l£—³	~3Âó8!ñƒª¡Â¼Bù1·ç†u’5Ì|‘¦-Ï+ÆïUç´VM‹MÁ:ZííH±ÒlIKSã
uM…œQõúõjkåK¼ºb\BEX/4ŒåĞCk?uîĞÇ¬%ÕÚA•ø-íV¬Ééi4:tÆ¥go«½İ¸šL~‰QÒX“SªhI©±}óéO‘°[0F±h7›Má‰CÚX“7+ò÷Z+ÎZgQÍ¥S]³œÀ‰©®£óR©à©šs(›òÅ@/d½+.£9›][Ìê4b×n KüMå:±¢m^m¨¸¶$²÷I3üö©Õj«3¯[NÙõÓc¦+¯^³§ù–nJ˜ÚÓTY´RIÉ‘MYìU×Dÿİïà6¤*5y¯q4làFÔ¹›täo:gòı8§4®ÿ(Eü±·¬ù²ù5Ë§¥¦4œñpÿø×Ï M£ È…³²c;ñòÿgLş¹Bzù·,Ó1½­TËGu“VNƒ¨F>°†pF„]ƒsËkÎŒºà·`øR „+{(©`y¾N1Z¶NÎô‚hK>Ø››o; ×Ùp6^tGÕxûwíøõtğèYAÍ5£‡6à 9  ‡Œ îÚˆ#ŒĞ/|H9?†œR2‚ °c×.ÿ¸9L8:2TÂéŒ<Wx‘vNªÔbŒJ¸ÎYÎMiì8&ÕÑH°ŞªÑC’FPtîÚ‘íé€Ô³‚kÆ}0¡¥§™Í`ÀõGgŸ”ü ğ4 ?ªeèÕ#÷m„Á@½`Œ(p¥(@eƒzOÈÎ80âÀˆıâ 9Vjè¥>#8@:7âÀˆ#\Ä¨\|-'x’närî•K5ú¡D×›–Ğà&F^å;ø–NŠ&7Ö6¦è…í¶ê©¶—=…9å£¢³ò¤˜¾İSIkfV@ÈD©å;+şøİwßMŞgß'É'÷a’%“å3š”W}âß…­ÃO`òÏ#ú¢aÁ×ğ³ã$Ê^ö¡©ó® óá?Ÿ Ûï@q.ùÅÍOIö¦ÂÁÛ4MÒƒ#ÌŸÆ [jçNC,TEé’;-ÆĞ¦Š0b|“ÎÆ÷P e4¾ŸªñıªœMµ
}dkŞÛ'yaŒàÜ"#ˆŒ Ò/ˆüÅ°1A“8¿ŒÆÄˆ#L.€èîº¹x	I·fÆ£€=`D€zŞÓÃ	’Ë¡PİÛÃ¼©¡sB25@2É$ıÆ%Šä4æÎ	Y¾ïº®o&´Pô¯o³Bû¡"?»âCOnŞ.(¦«~^!±¬å•«W¹¼¸)–ãF¼4×Œ‚èW–ÎR5áøC7>²+Ó‚›;€o‘_Yz £ÔmöÄƒpLG{j0ö”&§IJ‡´‡°A{‘`íQôÅÇğÁp¸ôiÊkÖwúŠ‡¤ˆ4˜ÈÍK7œ›±ùIïF$‘`D‚³  4ĞÆ``ã-7`67cà®0bÀˆ»`	åYÖÜÌáúØq‚^5|‚g¯«U$ïó>€ƒc]5xF@$ªgå†LJ«?A/ô;K@µRuÜŠÇ¼ôúúŞİïAL_^æßzı·,‘9[³Ï×OYZE¶ójşnôÓ{`Ô½…¬=ÙÛM”LŞ¯vãíî=ˆrÑc&…cıÄ*^QÜxk;hGWƒ¤0S}u¥˜¹¥ÏÈæ}Ğz06ÿošM`Árì"ë+Yºš­wĞbÎ å+~ş’	ª²‰s@âLb}¸A ÿØ³éPtö›öÍÔ*ô#£¸šl‹Å|¨Üb'ã"cn¥ğóó*ğ5>Ô^e?BŸ@ÊÂÖiÄ±Åÿîú>ì¿àŸëö2ğ÷@Ş“‚¤`ï®ê]àfüüúÑ1ûo<Òp~ï&Gøß#ÁÙs»2ÀrÕÿ†Ş›Ï«ã§çõFpç|G6úÂfg>–EW&´¾‹ZúRy¾¼}êèœ‚1Í¼ñç«UHß3®jÈ"onVíT²€`n‡@›àbÑ’àl=[OWK‚IØ--ÙÌòl[ú@±€ZûÆöËÕBúf±dlíˆÓÕb:w¥—KWÎ²Z.]èY›•»Ô¡ØĞÆ^¯ƒÀ–>W-ÚZ‡M¬D,İ½Ú†.oûãí¶c1‚íœTY=*•R _I‹êT«jÖjÍWPùbİ&­”®Î,P­ù7€ì¥Èî+õ÷ßW˜q˜éÈ”WİÄ‰/¸QÃÑ1X¤Ä
5eÏ«[åÆ~¢Eg
‘õXñhÁÛcÂba¨’ï."¯Ï^âz”ˆK²7²éŒğ6±'ŸokfB?]q€ÎTO¡‡QĞtM–EI\Z –p1Uã&æ%¸ÆüĞR|©ƒ6ñ8×i¬	|¥>í`ı†‹¯$"¥€E¿“Ÿ&Ô„'†ŞE$ÈDĞHÔrx!Ü	ÿŠ²a1
üSFI‹¾~®ÏCe{­­HãÎ#¾\)¾¼€Óáß¢íwwÆĞ‰‚ÛÉÿ––ÃON^ˆ¦ÛúV´·Vİ°Tíg±ã)îgs'ÿ3"æsBL=”6]bÖZ~2ĞtïÌÈöÙº±zg?u¨¹L¸‚WúpÈ@ğúŞÍ~ÿ:#ü‹0w¿¸h›”€öÊZ»k{hR,ÃØ©cÃ˜Öøü§	jê^Ü`í5‡upS¸äÑÁİgš5·«cB>6¨¡‚#	Ú¤¬¥©£ûÇYH‹2bñ*¿(Ù[QÌ@¹6†jaànÜ•·º|({ÄŒF·sŸhúªÃšç#ÜòrxMZ8¥õÛr¹6GÖ«õÂ^=Â1˜QşGQ¨(kh³çÑF5»‹¥fpŠçŠ¦“¤§«Ÿ?®ß¤‡4Úó·Ÿ¯ã8ÅÓm¼¸#dV³Ô™ëmbË©Xóp„‚¡B¶©~#~ˆ›»6ü›úâ¹0ˆ4Õ‡]½×[êöóz6ıX]{¹~s£.|³½H
¶XÃ™®¦³ÀÌ¢ß“ÓgLA‘ác°¶4½‰¾›E\Ä¶¥òTm!QÖ,X=œéÀ™¯ı:Sd°ü•?Şñ¨è»øà ™r~üëáŒ¦ëêğMl^wf>c<·´Âi0÷‡è÷ØŠ·†Fƒâ­©9Æk.­ëQƒJZ¼’Íy×Ô Ì×¡læ}[Şã	ü<D]÷nÏ³>!Ò?Ü"˜3\–½Ø,f£áÒ'*OÄ¯S¬ÔÙ‘LÉGç]rÚó³AvÔÈm*o£ÏjrªÆ²ª‹¹·\(&+ú2ıù)É~L‚(Œ@ËŒ]k7œn‚ÀîØ³)®¤ÓßkÚ+•I.8WÎ!úøOXaè¹R¥óÓË¤PÆ”dU&¦<ç|<Ó~|)îPHD’²W­««@I7kîRpc»Š;¼†(5ù%8+šì
»’!]$Ğ5º¼hÍn ¦‚\¾µÙB)oìNÙÉ¥	AK}íS§åÃ++Àıüo/Ë#ÿ¯ßW&âXåM¾/RaçUXäz™BkïUL5ß6@¿|Y/Ò¹ëSø|[&¥¦ìmÓätxíÆñ‡³™ß]MN©şŒ>›ÌÀ÷,k-:=ÿ1OÆ{;H:òùìÇg §lA
5îœ¢’8¯Ò`/"ÙJ=)‹½JLPiO0Ôú6L4)ØN+çÎ™ÿxí™´P†CÇ2Ìb”|yhd
4áæÄ¨`Nû,}xÀ´ÌíéÌ_ Á‘¿Hšvı#}4÷ö(¨Ë~—¾›RF_µ³ŸÕİ,é^¶¸I»DqTËpÄ~Ô¶®*Ìğ4‡	)-?s¡éE™KÜË£E‚à:Ëx}qÒ‚û ?$UÛ‚'C,¯v§õdÑ™™BºÜW°‰"Yàš5+­œDCJ^$ºaí×¾ `OÆ !Ş<«}Wö‚Ut—FsĞ.Ñ+ÃõùŸu€ú\K¢Ët†	ÎhM[ä4rÛ+Ísş¸ÖİÍsOÑ(TO±“O~°(Œ¶&°Á÷-×[‚r»‘òW Bòµs°4^"/‘8NÜÀ	Åò‡~h|N>ç°s¢½—|•–óâ68;‘ıJ…	4Ô¤r¢Ãç%W,’½şƒ>€¯‡Š°´í31uT;»:_Í|;[¾pAÿ¯X
îP’«ÿQÍÀ&6<¬•}Íœ Ù¹ÑŞÙ»;q_|tÙePqv.?¡¨Äl]ìÊÖƒÓù%?J‹5¹´\²£=pN4¹ÎJjŠŸJ\6qa/>¥Ní@rÊšÊFAËæ}ğãä4•İ'Y>²ˆİ‡†’TjËNÇ{Ï}ıœËÅÑ.âßYÃÌDEM"9j¥ |5tñ% é»Â"T˜ø`_×ò„*	Ñ`‡ã’YKÏr"8‘@ˆÛsãS>7rB^E¥ „´=U¥²¿¶(T8Ñ§–Çv©¬{Ê"2Í…·QgÈMı{ÅH…²OŠ~ûP-í¶ªEs-Aàp„MPşµT2WÚœüÅÒÂsMÅ¾ '¨¿ŒFûvÚni2¸ğÂ jEËè|†|¡Ñ¥<÷œæ¢?¨#üŞhş‡#Šº%“-ï
¥ü9¶³ò¿Ôæ¢Åì½ÀÒÜz54v^pûÃ4µã"#IrÆ’™L’úsn†+”¤Ö’©U6§$UÖì1şŠq%©d±İÓ={EÌ0*…e¦¸í…ëäöšÖ—m¶š­ğ’¡§“&š±ì´¢c‹OgL¼!¨S[`êT—˜ZMˆ­É6M0Ff‹°¶§NB“´E¬¥ªué‘7`ÛUÇv¡–ĞWÍ]ªœÜ¢rÅ8Öª_±™uj
Mi­HºÔÂîï@ÙLo@bËk^`âk×ç-í&8‡@{Kv¿‘Í•pÕ„~†=µ@úFH]à¸ô@Û3¬Ğêƒ¼xAà\õAÜ–Ü´àİµÔy§N×Y_O§vÅ4o’ãÈ–KL›ó6;\´Ò",öuÍS·Bòœë)S¿ÚäOŠ\J´ä¶·dŠÇŞğ¨»†=»º°-Ìoå¬F3Ë[Ö¡Õ¨¼¾­øµ5-ØıªÛy\õşf`ìó6z,üÜÆ‰w«zùìÙ6ïéójco*ŞiciŞ%m^
Şm¬#q>›ë‰=NåzŒ›©ÚOÖ·l¬'t(Uk±^dc=ë¨Q;?ÍRRuËs¡jŠ;Ø\©â6:~*"$ñö:9tä(¹xµ*TĞG±_×<63§V‰÷àÔêqn[3ìUœµNî˜ q¡‡fĞ¸e&/E¡/fÒÛĞ8`f],¢ÈëÒ–#ÖÓRĞ™¬{ÕX¥âSus›³À{R]~0œÿÒ+5±ËÔ«W¤ú j'7G‚L%Ï§ÍqGæş5u-œ_’Ã€¶¹Ôï]óuûÜå©»g½šz«•í®Õ.8ÉHGÚİªö¶¿™mowsDßDG×‹Û^é€½\‚pÑ’x»+å
·º¹Eîõ677®‚ùyfû·Dä§ü(WñÇ
*§Ğí×ĞÜØ&é¦v×	ÿ5Ò¾òò‹ÜáHİš¿Ñ7¤˜ãteÀõÛ¹¢SŞÚ¥3SCM¡r{t¾jÚC{÷¯¥‰óâ(×@Ó¬ZÏaWÙ¥€hT†)H=ïBBª8†>!øÊÖñt8$©™ëı+é-BÁC¹i69Ü'{àìO;Op! æòÆËv´Ë—»hG)r—ìÈív¤Öåº‚Ö0 •Wk»­9¬€ÖßÔöÉc+	[RÁ¨xK-2F½Æš¼Y¶ÛÛF¹höò‹VLgú”Úú,H˜>Ÿ0Mv¯ÏW³¥P¥ÑgtrP<l–38Û4œ³rN!?ã¹–Ì{ÁÅ¦£ÂµEŞŠ˜ÎG¸Ê 1Ü„ˆ"‰"Ô·Èù´z·«]áÓ/å+§¯—ó0Ü„AßY»Mšé˜ÊF²şºb3·¸?X[Ö|Ş÷â"LÕ<]÷d¸ATö´g÷œLğ£8ñ’½Îw®T¢|8oÙéİõÀßKÉ'¥J²B¸OY!d,:h»öz¹˜Yj±²¨Ô‡wP/şH÷Èk2íâun¼•eı>t$Î.ğmÅĞh™p;z ¬}¬—úôğ+Kíˆ.¼Ílª†b%+l6Zfå^c MüZ~­ER¶…¢¡2Õ³¢9 ì~á6PÄÃä¶İq«yüñŒ¥ªµ[W¡ùSİëlƒâj,–¿8tœÜÈµmŞ[#ĞÛ¿Á«Éè×"÷có”*H3^ë2KÁ#ï»1U‘Ç 5	(¯ŸA`//ÎÕ;ò5i†2—iwCÓ@,~
õŒõ0
—«’ÑZ´àöo-È°@#pò³»ßIşè‚ÙÔ=Èù?  fe÷«•ÏA•^d#¨B±¼Tş ³T®§ºT¥5Ì ı§Kóå¾ƒ_ –jÏ†æÃl—úÄêñ(‰PÕçÌòãää¹×ó¿:Ÿ“¬.3Ö2btˆãß'‘/Oz•ÀŞq÷Ç/Mi¶ğ9õ€œZ–<ŞŸÂRmjñ>
€“‹ËÑ9í³(vğ°å“—2€œğÉ;úiä¡ÃñÉ>~$2$%ÿuŠşkÓ‹ú[”İ¿…½ÉĞÁü˜[~à[¤ö€b”?ái¸›éPßÆCsQ—ó(/ LGÆçÓÍ3iÂş:Ñ1±•:Qz‘‚ıôÇŸ v".Œ•°@PğÀä»ï&şé˜%;éª~÷å„îªS·‡¾çN¼mÏÛ4^TV¥2ëBe¦FÅZ­ÛS•©dÉüZk+*´²âX,«ÃX,.-Œ…VCö¼R,3¹ºÔn‹İàAvİ6tTàWÃJã”˜İ‡²vÇ#o±-¹\¾µß¼zwùÇïj"+úÉ¶±"IÔÄ>']±IdÉXS³j¡HjÙL­²½¢˜­Š³^$õV¬ÙXµe]É,Iõ;í¹#›CvâY³GV|d)¦+™AÈdÔá<Ñ5;çgSCùĞ³ì&Ë\x‘¥l{™Ïd ¾²s6çzÈ ¾™Ä…†rİ_wì–óŸÏQR£u[Kªö-‹™•g·Xæ5j,o+`«QsuËc®Fõõ-½•7·RÖY%´´9ëÔ‚‹Ërc(B”	‚B®jxRzüF<zx(¨BA!AŒNÈ×ğ:Cu®ßò/ ªšœ…XŞ	>´3?_­çöâÕÛÍh~ægßæçhF>93r´ßFûí	Ûo£ÖƒfÜš°ƒëÕ|eõ~hs´ƒ¢46OÊ°•ÑPy4Ce49›zÖ„µZ°&ÖK`­­åhMôaM´Ó¶
‡rÊ5^¢‚ZéF};êÛ+Õ·j:§\ŠÈÂY¾ò«¨“?Lnn&ßW¥hTEÕ?)¨"zlÉ€*rÃÕ|¹’ŞĞUQşUQOªhT£Z¬Z0é8X–´{LG´ÑºZØ9b§;[ä¢{•d?ça+…«…Í–¶nƒ”õ_"£DâH_ŞÃ8IÂ…àŠàk¾èØáøºÇ‡É½ç°ãŠŸï£ÀòïqÅ|˜wäfÆ×¿Ïß¡¨¡Ò˜‚¸f8¿ïo¬AFóxŸá_›3÷•–®Ï3ù"zÆRù~0[,gŠ	lŠ®xEWrı¼j™$h3_ùîz®˜$HJŞZïÓ4ÓSÛ
íP1i€¾ÙÜ=‚¥î5…OÍz–Í­ºyoa†QëÙÓ6¬v*ZVLul=ééH¡]]‚c-“§Ó#Ñó-Tòû{Êï¬j5F™¨çn/tˆ*C<¶×`À^ílåüJy¹»
Ì­÷Á£íS‘Ús„çôÌLİá–ÏÀYÒòúĞJõ™B’ˆJ/.™+¢BX2¢ü½Eæˆ:íÜ@¢Jy˜y$F­VKshZMÖÏY]?‰Ât–¸)ÏF£ßSz#eÔè}iô.B6$¢¶å6OİÔ:nf-êÉNİ¨şÎ¿Q¥Œ*e"kvÈ*åJ gŞ|—ÄHqLÙ	C/°}{#p!²m†¸+Y
ÙN_Äùd‰–Oæ›ÓÉTUu89ŠÃt6ûÌkZ°ÊµDäº%)-†Ûj»ğz'ëuşJ:Ú¾THzØ¼WˆXSúñ˜ø²DŠdÿ0:È¿|^
½İ&«D_ú–}?À™ù’¤‚ˆøû?ğ‚9™»•O€â;ã}‚KËØîŞÆ÷-{æ¯Ô¶·Õn´ÛQ[Î!˜­ßfHş¼4óÍî©1“Ûë~ÚA"¦ì·p¯ÈJÍ½‚"¤èZi:U-ïá‹Vüp¥Æ8)–Të¼‚@ƒÏRa5xpéyµY~ÉBDuÄŠ2 #@Ü¿ŸÙ›ª|—ĞÊÕ:/	]²n2œC’AI¦ 98IFĞ©&ÊÈ6P<KxİRO-="Ä,Q®4tœ€Œ¡wRñÇ„ÒeŠæ’”Ú8õEPŒ<¦ñş	ç~½™ú³`-HH…J¾ôµ¾.¯øÕér®¿€8àËh‡ø&´B¢34€yEKŸÛëC°,£n÷¾İÛDLøà#ğO)ø»ÑV!|@bkö)°,-sïpZÀ_¸‹PÍ'=»ñvçF-Ÿ£\Ëéf(¬-ÑÄ¦Y˜™Ø^]`vö
^cWW_yÑeä	D°g»Ó¸ÓN`”à´_TÌ•Ê‰tQ±?Ö÷û]xPW¼¨x ­® ÄŒ C™[Tî 4®¨µ°Wïú‚Í¹…XÅÿ„Š·É$ï‡ãÂ?Ë"¹¤È÷UlˆVŠ4l@	G~STH¹l‡Š
è¡¢úª¨˜ö0ÍÏÓÀ‰2–ÉÍª
wéÄ ¹ÊWô*_iÛì{'¦2Æ.;]š°vÉ?£Üıöäšy®Ï›…Şb&J7D4T‰9~´ÔsªİºØTI–Q¦òE^*Uq…¡6L@)ñšÍ€b­¼Ä_îÁN(1r®Îp•>¹¹ bÎ\‡–eÍÕ3L»x¼+®‚ÍÔS}ŠÓe;Šóõ,°WS_ƒâ›Ä?íÀ>û)É~L‚(Œ@Ğ±ît
–Ğ¼×èDËKœë©í­ÑÒÕ¹6ªàhÓæ{õ°«(ßÓÌE_ÃÑ	Ú–T}|nƒùø„n¾Ö*[Sa’:ş½++¾¸tüıñi«¬€Ğ/‹²Xo{Jïe@`’ 1”Ë2X]NÔ‘şá÷Õ&{‹'ü>:¾¤Ä^ºAğ‚ş¥ÕĞÄu"(Ÿnü Êu&xjÊ@’j?és{qj‹ÛBÒ/±'Y°gËIEBß\
KºY¾È%*![½˜~k–¤jªªÕHJşßF¥{Öµ--5‰ÄÛ¯õ§5›^].dCZ‚ŠA‡^¦ q€Ë¾a¶å>R]Rğ$Wâœ¾¥çwe oNóRl$paş@+šö*—Œ6Ğh1¿Û@%a¨¹ÏDñ‹
‘ò+µ®-…]£2×Ã-·Ñh¦§h4I¯†ur1Äâ)ÜÎ%öªl²"6g"BBƒÏË•ú@•:.fÔw±•ö|NúrÔ|h(]õ2]šP/t#hÔ/£~1«_FğÁ»òëŞ} iİ³	p¥{Şp½W«Ÿµ8ĞëGçõ½»ßƒøM† {_åÄRs¬8:¾Có$w2¾úw”ª$9ÕbÉ”;w-RK)…aKyºñ¸ï&	\æÔ!³ Ùœ³ìn‚bÚÔ5³„¿¬‘l'ş?•™©Ÿ	OËy¡Ïƒ5rªÆNÚÌ€e¹aè©Ì òÙ½İ²‡vGCìér¾\ÍAÇ´< 3÷|v%şK’ü@¥`ô¤Štõ{=ºR¿ÄôÉYS_-PšÍM ZÑæ>dzkåä"Èÿ$#ædšMXƒè‚0—ø«}ö³¼E2÷â R©êE (¤Õ@¡c`ÕlíËCyGµ»ê² âBêÙ‰=VÓ"2Ü”Û{Fy¯¤ĞOºwUæe¦i
—ªVÂ¨Z¸M²Å&­¸¥FäuÃí…ñ3wØPèÿÁdà­XÁóQÀóÀ˜ó¢jN
XºÚ2q,q„ßáRÄ™RÚædÅø §¬?jB0çlç?»{ŠIÑÿébÄƒé3#¦h.ÿã<˜Û (æôEé|ñƒÕ*Ø,ºP7ôÎv¿Ù0°•$¡iâ>ê*ğÂõr5ksˆ8~²ÛÁ AÖTzĞØİ;)&f%×ñ¡hÉ®æ!ÉSÊÇ?Ö&‰sŸ?Ü'Y‚²4d|Š .
‹ß}¸› ò–¬ÎAaï”eÉ?G++B–Ô‰ö!ßRÆ‚q‹‹‹¡\Ê¯“.¨şË}'#ù@å'şW²ßE{'ˆ‡Ø}p‚Sê"’â‚îWAA	ç‰Øö2ÉXª•ÄÌG­¤L]å„ÍaŞ]Ö6ŞK0 xòû
²]ö=è$tÓ%@ğèy'ŸÀŒ‘İ±©XÂ‰	şÿ   ÿÿì]m“ã¶‘ş+“/)_jËæ»¨ä²wãİu’‹ílíØñ‡«+H‚3¼Õˆ
IÍz¯Êÿı ¾	" ‰ÒrªâÌA Ùh<ı‚Fƒğ*À:KRv	¡ˆ¨PÏŸïfÒ³DK„CT‘°wâè©I%Bc–5˜­2Bû(‘Íİ!˜†ä|Ÿ:hG#j×té²EÑ‘"PYöAQŸ··[â¾"õ‚HÚõŠĞ“£˜GÑªñŒ¤4äÒj(Íjõ0I0]íF3¨uµYšC@­x¤Ô¼TÏ(¬Ó “’ĞE ©B¼¤ŒÔP• Ù_¤4Ø¤ #åE
VNÚİìáJ”ùG‰³*ÜiÇ6"Ût)I¹ÇJskO8İ‹ºR'ä_Ä­:¡`ÈÅ"Nq·È~d]¯SZæé†	Ûm•ôˆøG—¶hJµªtm
Ğ:Qæ:6˜o¨bQİ(P¹Š¯?«“e`ßñâ5t,¡PlÎX…qoÒ4‡ôUİüÇ‡mhalq–eæ"ê>ÊÿšD”¤D«µï;k…U¿ÃHm$›K|´F¶…e¤]|¢/®Èg+rjèùdiHí¼®ñšsOI­jqi.0ôÅW±=Ìéç±sM*Féÿ´Ë~¥3=ü Æ¶2µs­û¹s­½¡»Š°t¹ ¤h·±R¾îù¸B-Q’ Ãß Ú¨ªU%Ÿr¤Gqe}ˆ‹ÀX¤LÎo…ğ	n÷H C¸+~€?g[%µâ¡a›FD¹Õğ¨\?‡|÷†ftZˆíõ÷™uW¿?,éß÷Š8õ½—y=yÊ¨ótßkÙ0—Q\2û`;MÅ¥İº6%-ªE@z—86ùZs7H¨,QÎ&ln©Ï´ÌDÎZmRâ°SFf2^ÅJ9•Oã˜Ô>!saWŒã ì>¾Yô=Ü!LWaĞÄ®ç–˜AˆÔ¸ê®s¶QÑ%÷,&EwPÒ è<1':¯ò½gkJk„î˜½ôZ§\¸vß;w¦ré™tW€°Àon_`„“p”äĞÆÆj"Šç_€¯›KAçÅY0€>4‰Ô"x@í€£Ï°lÕéd*3É?ƒX˜å“àh÷3r²ğ©ŠN½OóDU‚Ã*ö?ğ(ft™?Ò‚zø>ÄH}Æq)•²Éö(ßr¦ö(#ŸŞ°×o vÃ^ÿ}şöhcÏ§*Áñ‹°¼ˆ´¯ÄèZª•õÛVm†Ù2Ú¨â…ì¾ )Wy#XJ´®XV (KUëÃpé~ºÈ¿¾Û·4
ZAcŸ|Œ#‚„›‘¶"È3Ò/R4O<®êNÊØÈ8EF¯Â°¡µíWÕfZ)-‡&X*ù°E ²÷¨û4R9á:0\{ÍÀ¦U‚SV¬œ˜:ğ˜qùİ¤é~ôÙ-›.,§ÓNÖÎét#cîô(™'ÊˆÚ0G¡z«•E¡2½×TÜUÒ:òs¼iËDaTä¹¤£d÷¸­ÏáªÈ²C®M²Ñn—â<t¸¼¥iôH6ëhµäñi6NöøvpìJ"b÷¡vßåÅ¾ş˜ó<áİ¯æ°æQ.U†±§†‡‹«<Ïk\ğ<ïü0›¶'t¬Ó%šDC}bñè3'[I¼Jw™7J6Mh{qYƒŠó\·ô€ó‘ŞóAóqÌ0·„a¹}S”‰ñæ	ÉÚ/L˜ÎL)³Ï†è´·N¯Óönıè­˜x­äˆ]LS,y†›âó~ÈkH½/Ğ;Á—LÍ¯b&ùİ:×;9²sKñ:ôqıC¾9Ÿ$ü˜¾ÏÒ_eWy&ƒÈµºĞÀ7å†5CweÀÈVíÙ"b"µ êÏV».IY’ÈÁ©…FsŞ˜h’†±ĞBB£œ Ä€­Ú˜«x>ğäTbÖs˜›Ãyã§G^Ë‹~ä(Ÿ&‰ƒ^3Ù‹n`Õòv=A;ŞåÅ}Òù—"C~±*RP¢_Vè\Vè²ğÎ¥;^Yy:ÏiÉIşuz6¡97ÍÑ¨}Ó1şàsNß—q:zTæˆeXS©i;RBVyÜf*I¢¥Œ9|¥SÑĞê.~K»R:Â)®Œ|àDşŠr`a¼šğ;mÔğüuŸd”zİììê:6²Ï=4B<ßÊª¿F#í"ôË&?<>Â| áh´§(ÉŸ“<‡‘p_hêj2Ú7ïÂC^¤Ï›>}¤œáÚ;­æˆğNS)^ŒƒUİ&jé^ì†bMiÛœeÛè$ÕkEC1Mú¥¢Äâ›0&}QöMººÓv¿†•uÊX™Ò²xB/‰È\ÉÕGoéëå€ô"%ÒHm†ö[ÕH4l—Ùä­2Ù€ŠBØ`ŒBßİ¾Uª³„)P2â·t: ‚‡<›ÏËGé-*ê}¬ oAÓb›µ7`á;¢)¿$ÅÓ»&-ƒ^Ú‹ÖAU˜%{jÏä¦ãÓ¨Œ9Ë6zXr'½¯g6Ó{/óî§SF¾¥şŸÿxY–DP«5z¢¨ùM™¦E´TjvBJõ&'ä§K†ÎsK›=^ &æ@&Û‰ÆwôÛ8
¹AÔ‰H8ı¡½^­mV½¸[uù¾À³·øçæİÊ/Ş½¸ü	ç¡i¿BçC¨O+BMË®ıØáÊ¥ÇeÛøªVøDØK¯BÅ"|³%â#z˜ìWÚ ;¾]f~LÂÈ×èAsêÉ¶cµ­oñ£ÉãS‘WVÁÚºÊ?n²“¿ÒOv/ˆ1ë Üp¨ßï£„rBİ ‡Û˜ùpãş›%Qåò)è<9~ú}ôœì:_ğßÆ>¼ê¢ Ş?yÕdõ)‹Y>cv™Ÿÿ’€MÅ}:0–²¦%?­º9 ë»©ä„(±=Ã¦äpÊ’?-Ë
MßçLş{”T³ØÈ†dN¦š‘,Õè¦-™ê~dºÀ‘şê1¹¦Âôå¨pœU'PC…,/ -hÆbÅù™T¶ä¶å¯'-''1b@ÔÅ7ÈY=)&¬vb-‰¬‚Ñ.%ïpB;\[–ˆR£(ã’
¶(#FédMš¦!Dõ]uä¡Îä¤ÈŠ¡g˜‘´Õ$yJ!` ±ëSº"<“oÅÀ_s”¤ANC31V…tà~”¥tit¨LEKì¤CÉeæ?=±‹ä:WRí»0£8Rb‹J¯¶µo®­Èã< Ô'âràÈ[IÄaÇ†!Íˆï‘+({<ÊˆA(w'S`IŠ¢àAkšİ0Ñm´Ö´9§`"Ö¦á®b(=?¤ÌÀdaôÃĞ…ŞÈ’ñ÷*#NGjhß3>Ç}\´pÅGGšß¾uÇÕ}¨N|Ç¨ÚÒ©¦øÃû7_—ÛÒ0Ã$İ	-£äu°Eä„ƒhQ ±
ŒÈ€Sä8ruƒÛ.Ö«êË~WÅ¡Tì¾ 	!%h´KJÁ#AÉèâ ËŠŞıÌÇI%n!¹«M0qœ
{½òLÖ^NqHÆpêÒĞñ¥ À”å‰ü5õËÓ0BcRnÛÖQd}6«mfâ>Qå„
{ájmÒûµ³òïÍû¹aıq¿3•IBú»¹—ĞŒŒÅ’%¹¡.è(P·€¹ WtjåS¥šÏ$º-;ù$Æ;¬ŒKxÇP'öŒU g—	º`èXÙ²°¶€“Jèµã%¡ÃvÄ`v÷3Şt,k—ŞN~íN^G¶­Öiö5oÀ¾ıõ0Å…¾€ØÖ©%Š¶Îw™âŸCÂÉ¼áêL^'³Xå‰"±0á;¾â°äDñ‰‘ËêóıJÑR»~›¬Ç¹­ßæt§½†Ï0šv/Ü>‹îòä4e§„ÊñÔ„NÏu÷¥Åúšµ ğ–ùª\ú¡u’ç;dı6FÓQàe<v•*á’áÀ*…M­Nxë~gÜ¿]ë.F*¬ğ™1N} éXw{ZO'Æœ.;‚,ymAÙúG!Êbé:ó^ŠÈí>¶Â(ƒ'Bvı È¹#R£¥8$zQì¦C9d¹@âmAâõ¡ÔíaÍt$0­‘i$hÎ\?L÷€ÈaÃ‚ÂëâX˜ıVÈ%+şíN(¬zå0tQ|
¢>ıt•ÚûÒ"Í«‘ägšS”B!>óÍ¡«kÙ«»]´1R`Ø¦&Û 9zı¶Áù¼„ÅY43ô®ÊèV~„fç\š
5‹˜¿àÔ26b]2êÊ¥ ÿ\q~œo£$õæôT¹Z¤9ñ~Â¥İJ¸	‰œ¨e§ç< ¤dîpSFíäx ¡cäî+çNƒ<-r5,ÊUÂ•Dh .Ä57Gğ
²ª”›‘[„üYU
×%«ê‹ËªâAÌ©§ŠFğu0ÃêHß’gE+›«+`Ğ”÷›æ¸$¾/)îSã’IëİRÏºVNS’úK\9³0 –eÊ»Loå<J¯f£oª)-¹D–È ã%‘Ób¸šd¹.Ø»Èò"Ë:eù¤ŒºQn*½ÏÍàrÍw©\ úÔJÌŸ	Y¡Üµ¤Òì¯ø^Ä¯Ÿaö‰q¿¢9#¤‹5i30 òèO+:Ú’:N+©ë½¦Úû•ƒŞÙ¿±Èşuàl¿h°ÿêË
Ì
œsÁè4•€v÷%Nioë¼‚6²;Û÷=—óª¤OÉÛ4<<C¹Û¹Ğè`½¶L—óâÊğïŸÒ"U{¯…İZ/¶`j+”4ˆ)WsİWöe÷í ‰î¤•RÓŒy+z#‰TÙƒ,YW-õdÈD5ä¢p‘ºÚªíş|ÓÏ´Ì9în®æÿ°ëßT|Ÿeàó÷I^ü{¿ç×w{r˜
Œ/¼&Á·×¶ª_WW©¾‰ıæ×Õíô7¢>È#Zzp`eêz#†‚w„å1—SØ±a®ìŠİQKÿÇo¸$q"t+‡şšˆsÜPEıÚvyŠ‰D ¾–Í ]Â†P°ï3ş	1Å_ÓP½Ï‡?şñdBÙ¡¶†{çï¯Á©ş0 qõwiİCk‹æ°ÓªşÒşw’óÇûN9¼K^+\`œèZkö1ÖÚY¦–ßXølÑïaö ‹"Ù=R-š‰P†zš¹ˆ`{È˜‘·VAï1î5 áÇÇ‰`´	ÓmÚï7Ê!úêhÃÕ¶xJ2Î¦qzÈŠ'¾¶èp—'ÅgêÓ,Å¼¦|b••p1Büœ!]ıYÑi“ôGSdˆ Åø©ÛwªöÄ/Õe÷%yí/«CËWGœå+&Î[©#NîÂQZ‡
hRkßõ„Z«QÇMrG¦×JfÚ´bÇf·Ÿss¥ÂFc«ë¥€uÁ[~¯„5®ÍØ©õÜ€—6G¶$ğÜ
¼a½ª§XjcêÕ€]P8ËıÁÚyŸ”dÈÖªÑÃ5|/Œğ¸x8t±¢Û³W‚]âßÛ˜ÇVŞV2„º7CÌ/ŒÏ*¾¬hµJ Åş¾&üöÊ‚qé¾pvAşy"¿KQ?ºGoYÔğx+êxM”éJ"-zvz–ı1éH¢,(óD‡/Áœ„ä Ôğ Ås==I¤Âs5£ĞÂù_˜Å€Y˜Å`R´Ø‹=Ás&öWVEº+@Xä›ó†#pT£¶i;Ñš’5zÌáÃ%™_ß=ŞT»v¢éÕërï¦2ßMgd°ªÏe»şÕÄôÇl÷i{È=^|ùÏÿ(SîşØæô¼ÙİÚ¥îØRÆ“Ş±Õj#ˆã@–>î¬2õ=.yolc€™pvœ[Ş7›ëJëäuá™•Hü¦gumÓİcIøçøwvF*Ï¯mˆÄÀˆ–Ci~A<¢%[•TW§ÃhGj¾õ¥s©¸×¥ ½8KŸß»?‘fI0Ì’|–“õéİ;Ø[pb©`ag¸ïÒ,H¢ë’ò$•<&ôHË)bÑ£Œ©Ñ[Ú•1Adô©ƒ
ñ9İF“YŒúâ®ŠÁO:£QÁ 0Üw/³R¸0ÕˆìwjĞ[Ë$8‡kÃ­ÙÕœ«”¾â‹]ÍV×í øª²BÈ|"'|1~«êxw=-J9Ï[?nÚ’âZI2{ñ±ã1Ô}\Rb=Ç!ÙÀíDÔÆÚµÃ¢µÌ9™ş5‹ÄO½0¸xcªäú+Ïl8»KÒY¢>pïıÚ qızA_år±Ã \ÛÚÓm%¤zIÄc÷C±i²ò3pLw~¦IÈYÌ™³™3¬ÖÍTÌxiœ¸èÚáÈ
_÷d‚kåzDŒg^Á3T²×ÅkÏµ)1‹‘¨7%à]Å•ÃæùY3O½Õ)ßü)ÍŠÍ}«E‘[æC„?a–ì©çˆ«‚;{üßŞ³¶@STÿ2´ßXò÷,»ŒåHäŞ"şƒÈ"nÏ»Xõ=ÏİCaçFÌ²#Säµ£°
¥—,ô!ÖBï•ÂŞPZú©d¯SÒ,*D‰êªd•ìHáì=¿1+5!Dt m-GÁo[Jãx3BiB¦çrŠFp”z±Mß·0Ì>ï\ç-ÏÁ#d–¬l¿÷¨£C6X³â9y†›âó¾¯¯J}˜£ê=	>ğ¿ÿçî#ì—É¨%/½'QŠşŞmAç¸~´Á#²èDS¸ƒ›İá9€ıÒu›8ÉòaÍŒØÉhP~m]M•º]<¡±7Ÿõ-†ã&w*÷"$8ğõh~åÛ¯¿ tÓ¡~|È¶T
ét3mœ8ÙByDÒÎ}Ë«™Ê,}I"ö4¾ÀİÒ¦AÚr+(ú—Â¡<úÚÓiZÑGTVCÎ]A¬CÎš¯˜ûC”ˆ”]%F·]Ç7€ËYA:ú_`ú>MdkÎº¾Ğ÷sÖœ¥ğ¦Êf“ß_Çnè€	Ã¿{ŞŸå÷@­W¾5aôiW †>€*h*±P³1Æ)®ášfäO ä¼G¿K–R4càG«)ã‹V?&F_¯ŒĞ8aô"—=0Ö®c;S èT
¤j1¿:!„Œí©\˜D‚£(v=Îšt"°Â”C°v#Š¦à²C³ØNC×r€cºS!æÂ”*€… Òğ•ëO™ŒR9N’ÛZ­}à‡S¡Aœú[TëDk.6—[ˆn2‰‡ÛHsè·ñÉD¢E­Ÿ&]Áî›È(È¥;mş-òçJ¿„Æzyc~áf2Æãíp†Æ1óš…¸ÎÕ.yQ±tûYùêil{Í«g:µˆß–á¦€–aî7æ$ 5™èƒÇ•_©B<jïF=¶Ö—îË"‰8‘TƒGB¯µ%¡·¯•š“PÖ_Fòy¼õ‘¹ãm[rÂlÿ
$iÙ/]kå’_»ü3-=Ì‘Ş]4ª˜Ò„"ú\©CÈeLU3X”cˆ<u XÄ¬ªãÃ²÷¶µÛ{øç$Ğ=¬Ó‹‘ó‰bsÂÓ#ÿJ)½Zü8ÛqìÄqšµHŠô´ø‡™kt”¡ñª¥A%üµÁF2çh0Å!±	.xÓ.0şY ñ ñL>ş™0Ö{ÊÍÂf[F3Ê²íU®ŒšLD¾ŠI<½RPÕ6;[‹£¿"Ô¿PÏ³Tu‹"’QDU»ñÆû8—†Ò«oÊ½=åPÕlƒ/PÅHê=ÛFÕŠ(r‚¢»h‰5:ãMĞ‚¾¬©:	WIo$J² ŞÉöY-	×º˜fïñbPeŠP“ÿ· :óï2äˆ2Ê®ËÒ’G.œŒ«>‡ Î¾êÜ*ü#~²´>M$vª´>b$ôRsîè*½²)¹bİfBÇ.ëùoØLÊxËf&T¬ÇnzºúxwBß_še|ı«­sg9OOÑÍoÏ¶±¡Z†wÎn¯w^İÇÌô®Ù{ªåIA¡â6Z·yç?Š‰×F¶³x<¥üğà¦Ğ
TÍ¡nØªğ“Ì•6G©9œ³8JKœ¼ùYÜ«^—˜÷<1ò$Ş(4VÊÄ¡±918Ã#cËŠV¶˜µ “§=G½JúôæTêb,šyFëøÒšù´h§ÚÔ»çô“¿ÃÏŸÒŒz9N^ÓYçd eeq,Û‹bÏâ¬L	ŞÂ-D+wÖ?r;XÇœõpÈaÕë'™ªõ4?…s­Ì¬¸†j¦°¯ƒ*Û#îúOİÇÇÂ[UÃ×íëœı)Õq5•Bn!Io?ÎT¿ÜP0wH¯É­ù0Wé~[Ã)è{®ì¤|SJâ_²ô°§Ş°ÆMêë:¡: 2HôâÀ‘ÏY*«¥iù1-~H£$NdáÑ÷Í8XóV
¢‘ &i×
—#\m…“*	Şl=ås?«Lc‹j^4²G¯-M­3xTÇ¡_ß=Vœ_“P7w‡–Ğ­Šã‡]ã6Çû;ÌX}ÅÙ<²‡Go<Ó¬ª]¢O«„œVs²áBÚîÕÚ-÷jEJª1(ËNyîÿ£ØÓå»2êé¤ƒ¿dh
¦Ñô1™˜÷|NÏSh©»ĞàeÔs¥ÛÇ8]„„è‰k‚Xƒ º‰§Æœ ƒÈ6ş-úcé*õ5É2­¾E›®F,}#²¼0Ô]QiË[ËëÕHåÚÂØvnC(oNDCtªbsMœs‰Íİ¨] 6×¹ÊÇ½Jš; °Òı+Ö•óµ{AŸÌ¼‚z£@-ÎØ}$e1K^pædıiİo‰³ôùÍ‘ÿ_uVPoòÄ<¤Îİh÷øš·‰w¥—}\Ô^¶¯v¨-ËR1xÛ™¦mª£u&@ÅÄqFRÅp^«b³‡ß ¼Á@4mäºñqU±½Û,%Ó'¡Û]—ªëÙé+1ê@QƒİŸëÈD“I°~ë\‹HE|Õ*¡¿E¯HİÑKîï¾*’üë¶rìŸë×hÈ_¶l/¤ƒEÿ.«Ww¿û	iĞŸ‹d›äÕ"åÀ!+»ÃQoŒ·×úUš¯ÛEC?š44ï»¦q#gˆÿ»ÜS@û$üÓ^:Hínèûj WL =œ…ÉÛö»ü]¡üæw¿ÿı­EBşğ‹şRh}Ô¼¥³ÿüÖûëoŒ(q«ê©&¸Uµl(SÔËŠ<èØî¢^bE•Ëyo-<¡toC0s2ŞÉl£~Õä¡ÖG>‰Ú•GÖ}íâgØñÖ}KÉ¹ì ,yÈ«ÂC[b7 ÖàÅïÏç2ÌY~Y­FåïM®e‡ã7ÒN|)ş}©ÓÙ³FÌ°ŠA†¬ŒµmÇ®îüãY.ÙK¯¢KH¹„òX8’úÑÊYÛ”r‹q³7£®ô€0ÖMe¢íÀ´]Ûg]9q!™¼i‘˜W…öªí¿[­¿{ûn±W{u±W/b¯ò˜ª2¶‚aEaèÎØ˜åB½ôÚ¹+µQúêÔÉ;ó;ß[}«»¤şYÔÉôçúÙswOL‹ù/!ÔÃ$¸ .-“<2Ã3£à$[láÂ²¦%•CtÊBcµ‚¡aÒk”¢ÛKÕh&uŠèxéçr4öU~3×£şÖszz[¬ú¶ÎŒa7û=Ùì’g@ëØÜ"ò´,¿),¶0y{ÍÒÃ.âi¢ÿ`¢}8µ}š'¸‡¿ípv×Ÿî¾ù&<äEúÌÚI¦Ê–Îíäv?²?l›XO{JîÆc'ÿÙî£Ö;ñ½—»{ïÔ=mú¨Ò)÷ó2šõŠŞ¡ë¦Óæõêe´¶zì¬gPÄpÏF%=z¯Y²u8Ã.V×EMÃtáF××ô°JÓ@] Ó4Ë°2©†US5õÛ	0×Nİ¥-¡ë¼Á'·¬WLM6çÃ»Æ1Ù„òxË"vñ†»œb@¸Çë!ï›İ%ÎûŞ´5Ë;Š¢E(>íş	÷‡â)Í’ÿªLwàÅ~ì¸”ôŒ29zĞº|¤-5»¶º£ô$L›<ÈÒOˆC¬Çû-(â4ë›üMŞ÷&ÄŞ%d'†o>E=@²g{¥ÕÌ6r{“p·7*iâvŠX¸İwyÜş˜ó´oEK,Wb-†-E]Èş¬å_èfQ½D.ÑmèMkçóû{!êªÅÆıÊ¥~f¼2jTIÔ¸%PÉÑx»ZxÆ6Ã7o¤„¯1ÕÄ¢ìyl¥GîCæSI52„‡~—l!M2€¼û¦Nï¥¬DmÛN`Ä_uä”ŒŸ÷ÛD²¥\Û[A7€RC«-kĞa¯ÖÚ¶‚×jq{¬™S†=Æ8·YÑ±Ö6ÍsänÂ'~Ì)QÔº|hyÇÕØ«äD1«KÊQO$#„UËÑÊ×®µºçß¸®@İ•31ô$ÿZÔò«uÅìi+ZáJn ’%—'®h˜Õj“¸²äúd™yE(íÓ¼¸¯¯KRà™›Ğ€”¤ê×1í°Øl“4YØO´±XmÂ¤øÌ~à˜u˜vEöy“ä)³sÌ”M˜F”epô©IÎÅ&$=iâï"N4ñ¯ÿ|2Ò<]gR¾·WB'ô"–Dá±­RfÅüïF¯Ò_}8ƒh¦T¨uw£ÒOOŸ\×
‘ÓÄáé6s#åEÖ‡UËÃ/ß§ï^ğ]PÌÜŠt¢Ñ†UæfF¸tŒépº™!“¢·I‚,*K½Û­œç˜¡ô"g]\65˜sH€’ğ#Ì µ²Ã5”•{£ˆ@©ñÚr,'â¬¿Î&‰H ûáPˆ8ú1V»qh€Éòc€š~‹ŸG’Ç§B¤1Éè­ÓÔLØOOğY’3±¿^­V±Êiú‚IjVÆ:°ÃJÍ»()ê;RäkËYÆ€_÷iV@äÑà”cL–=p@Ú¾yù9Ç7hÊŠô‚•8ãtc¤|Ÿ†ÔM">RÛ	"Äjh)oÒ‘½öÁ[y+s²B#–ÑOéãã23ÓäÚk-îÉ2\ò°M?ı€ì!åJ­şÎC‘"j—o{ûjèï Ûë84İÉË˜æÿJ“İ·Ÿ+|\¦LÛ™¼ O±ê|I?JRäÇÆjí9öTŠŠtÿ>•5ë\àqµ|©î8‘4Cl¢%i*Z‘Éãôš56\ÅïL6{	aşgº=Èª+=ÛŸ,3o¾K3¡Z×±áÅĞ2&¯q¬ìJ÷I()½‘Ù®ÍySÆÀì$»	T@/òíh5[zz™Ğr™¾m8F4YN:,9I±®İXÑ‚~ŸÁ¿&9ÒxŸÿšD”¤	Äg:Îd_§§£>À`.iÀÂ5ô­Ğœ¼ª~Şãl$Ô;é}f/ò€5öª9kCÈQ–-õ¸c›¡íD*íŠ)…ëš¾,E&ş}$¥Æ²-¤/ÉşàC35JhÀX‘vß=Í!©Ä]#\PQß'»0Â'T%±9X­Õt×°ZZ•Kòzc­C•ÙÏ»gé ”cAÃY“µye}NmxQøÖä5U‡p²¹dL®¨ŠiÔšó§Ÿ¾—£&,®<Oìş˜"ôÈ"Ùø áÄ&ûî§ñYŒñWÆ*²¦OT™8ÁĞôüµ1™+Õ*š@ˆçÄønÊPQhbW${ é¯Ø¡ sf2´T¢ò&İÒK$â8n:öd»"ä[~|,\”ÛšöRŞC˜•Ÿ¬~¥íZéYŠ“™t~AgMò2Éö`5|AÛµŞ­8PrmÛp¸sMA×6‚TÏ´Ò`¡üSíá¼+·×U~­åñ€:­­:•´İêä;uáêçšØŞt¯6É›ç 5ß›?™,ÄÊÈ8fXp—¬S/úÉseÉqôàn#¥é`xDÔéÀª_:W
Ï¡ÍiBÓÍ9Ñ#4M†Chˆá÷|ÙäÕ¿sœİ?ÒáÀIs­òÖ!®="—FáñöÓ/Ó7Ö¥å¼ÃTŞsÊ§ì9×Bé¤6éY-ù(iÖËjYVËU¬–NÖİü   ÿÿ  eŠ xœì][“Û¸±~Ï¯PòòV©](QÚ=v¯›=59vy¼É£
$Á1×”¨PÔŒœıï ï$@!J3­‡±L¡Ñ Ñht ¿ExçFo¾ İû¯œ­·»	îŞİã]ôÊ¼`7úÏFÉg´|Ï"‘\o‡ü‘·‹Fv°;DáÑ‚pôb4ùæ®gÆÌpf?ı¡Jús÷oïD!©ÃÛ#B¾Ï¿×)îÏ…9„mÏ~ÙíÑ-=ä{ÿÆÎ[!")°¬ ğ1Úğ7ï©ì?„gu¸¾à
óüóƒiõÏÒšãŸS9~ÙEóÙ³œÅ¸Àî§ŒÛïü†d5nY•Ï>#QƒªÒ'B<„^„c)
_à]iéó:Ó¤ş¢¸¡ËÃlûèp}¾ÙØBy‹]tô£×ôwç“w÷%:NÑ X3Ç]¸öÕÕ$–¯,Â>Ä÷‹=Ù„ìÑO2d;üP¥Ò«”51I[9‚õ¥šôSk£^–ƒÏ†ZËL
nGéŸMtÆÜáÏ_ğë™E.^®'Ó•S×kÒ oww×=ò¸6a’´[’úçcD$/¨],Æ³&õV£ZC‹e1ó¶·—ÍÜ›ÖÄŒîu©âjmš¦ËñÎ×ı§õÓ;Ç‹şt§©‹ÌÉÚš“E½‹R¶LQ¶ñj6-D5$+£Î&¬Hú^½´1Ú»Oe•R 9Qã¿íƒ0Â¤;ïIs¨şëQ|¼`ËZ¬…Î^"@"NÜq{&òó*´ÛRıS"æDšöŒ+Í£ôÛ›ì·¾'Í€"\Ä´ŠÛ¯2«ä)úpú~%|vH—ß·Dríù
ü¾ËQË‹óûbE¼	lÄ´Le_Zæqt(á’qoPÈjÑA4³"DßæZ3›¡u?ïJ‹,MĞÇ<øø%ˆ=“À˜–3Cn]_c¦¬wöôkM÷ã´3’ßõk<cDäf¬ûÖs-•_„v³–©h·4Ao»ûÏÁİY=Šî,–æÒœN„æ¾”`½VL´Ó°~;_¡"anŸ5¤2µ8¿Ë˜4j¹NéŸ@É¬9FA¢İÁ×è-Ù‹©éšfÁªV‘êì¹| ›ıšĞ]¼“Í„ìßmõáÖş8šöo‹ùz²6MN@ÄK-'ßWö#9äÎ­j¤6nb¢KR©¸¬Ô¶-.ª5Zÿ?·{ı=ŞèÊ­]{ºàÄhi×GwÂDk% 'ˆÖ¥Jvï!Få{İ«:“ZEc‹²º/èğTñŒÕ3½¿yõófR!É¢o7Ê–u#êİ1.uZë,a´Ü.<wŒû¾¾jšLÆt‚¦sƒc–›§ŒÖÙğ„ãÄçÒ°Û(Ø|_V­Ü‰¹^sqÒğ<¹ÀG›¥(ÛöûX×²¾@‹‰k#,Ñe¨Ø¹,Q²uñîv(:†Xto‰0Ù½Lëú»×Áw¯…íÃ?ÿ¨+9Ç¦»\&à‚S¦‚dïƒğ¸ÕdTl{9_q¼°)ƒÛ
úìIa=AˆÉÒÅ³	'Áô-æÌb2ıZ‹1JÑ¶'…ô‡Æ#Ò‚œß¹}†ÖÚ¼UòÒ=§Ô^#mğ	g5wL1~jPtîÅ&Ô*l=3gºšOŒ‰Ãq®n›–!ş›w õ~ÿ›ç8XSo-Ü…½^¸°eSuçİ²ÅŸOø_G|Ğ„ÅF®µœçà]=ş"ÊêøÁîn„öû0¸ÇÎÆú~®È‘^Xu¡AµôÑÒ¸HíTCAWj&m*4¹7uşuï ôèI;]Ç	ğjfO9‰_WH<^³(Í-"ŠM×³.%šñBVé@ÿF¬ïf{$F³âäŒ®uÌ[ cÔ	yÀó©=7ñàm¹Š[>â]Ì9w"1òû•59S‹ÅtµB3ÎBsçp.ÀVİ^Â9œÛˆè|¶Æi
ŸÏg“µip3¥É¹ÛäfM£,ˆPeÆÅ~ÑÆËpİx»¯ñfG“õšØ&²9+!Ûß5˜.öûĞ†K¼+£1õB·rõÍ™,•Õ`´ˆºi½af®ÜÙj	›¸«ßÄ¼Tš)ü„v_5Ù«Õdi˜“ÜcéHZºñ‘£Å¦SH„kòÄâß³ÿıá‡¡ç`­*˜®bŞ²Æ(;sJD¿ËvÅ€f2é=I/6_Î	¬”ìe'r<Y>ş„{¬	¡eaO°ËßãF9óæCÙ…‚CÉÎEĞx »&C›ş‹>Çüâ&ıŸZæzbò®%€t×%¥»~İÑ¢.h…aÌÖ6ç¬2 c¬/ÆÓkİ²3<1LË‚-ËµoY’‹9½È×f}×ZÍ8ñ8¥]µğİ6øÍ»%DGMŞĞ£µ‹-ø§ÈºA!‹ÅÑÊ‚ ºöX­ïW°È*}×óuí–d®mŞuÙùô3¨ôUï´ínÖ;6æš†ŞX3ÇâàDakÁ¼
„µ·Óxgá¬œùbÎÙæˆ/¹Ø“:Ê·xîèÿ
jÕ™Êè=ô»”xS®xz•¥zöÖËr¯é8¼²¶ —ÑRgÖÇF$9‡ğùó¦¥¯)â]Z—^?Y½ô*Şüo@lÔ
MÛ Ûš!Û\.!-pŠºhJ¼!"¬Ó­Y™Ó™53>‡KòxOõé|nê×¨*O—«õ„/ U¹.U)¿ŒAÓºâN×š#º¼
Côı†øUÿ‘_–ıbEE^>û¡æúÔ+(.úó9GÿÀ´´1	”§ÕÉ8K*Zw¡E"=©:]	†7OUOjÁY3Ã„
À¥Â.}¬{¯‘ıõ.;‡%X4åå…ksÎuw d\‡²'oÿÀ8Ôhæz‰Íl”3nPÃ¼Ğ º˜±×•\íÁĞ:~Ñ©ÕDããŒªnÅÇ†iZ<œ(>(ş ŠÿOäû{´Çº\Ş©e-œç=·ÿd(NÊ¸AãóBƒh|Æ^—Æ÷Î 4¾ıİ5»È»İ#M7s.w¾t\N¨¢æz£æÈ"5 ;J4õİ?±õ,ëµ·:~¾ù`ı†IÙÿÔÔÆ%/OÒ¦Ñ©¨†Äpq†øpô)Œdw¤ÑKİõàEö—Q©C9YR0QËµ;›Í¦öúÇZúÉÙà:…¼‚,?ãàc@š“Êô¬2léÇ"ÿZÿ)áoÏækg¹ZªóïÀ·œUqt¹cWáğœ)Î8‘fœeiù3'şÛd•šº4SK®¨Û t°E×$ìGw8Øìé7şNÙ6>6_Ğá7"õÀ}Ê/ûï Ør8ØÈ×m	³vf÷,$=Ğë}hyo©,T¢lõš
Ó.W)ÏFâZÂwÙp›[>`­¿‡ÖíuĞñh-ÄáÓ¢Ë¤¤ö[xBãúVDë4'"(Éè0c‡VûbÙ¤u¤ÕU]Ş—¢s¦<…;
Û]¨"•NÏ©È§7‡É]a{¶2,9‡eOD¸Å¾›ˆÑÑGBÆrJvËò,ÕÙI¸F…îÔêUú,S¬âpvÈ.'ãÆÏ±áo{4F³!J¸\ËZZ/›4 Ã€ö;˜éŒàÇKG”!×©Ì†™ó›ã¶;[{‰
‡4ûô2U‹~Ò_‹:ÒëèÕqU—_.íEGÇ(x<ìü€¸ñı«Ş’¨ªéBsÉƒÛ‰O¤:HšBoI©_å“¿NÔ¡îrˆ©Ô…wâW¢£ãåew¤iÂ¢û/ÁÓŞŸºî”cïvßÈhn¶èßMŒel.C34â"”S\ËqÏ„'…,¸1œ+¹ÙÎsKDßüëˆxCo4¹Ç›`Cæ!„Xï5S	_¤=:]£XáDœ3—BPàyQKÿø¢¤´eÖèÇÔáÑWı®»!äİÙõĞj×•OÈ¤³Sò]¹œ¹* œV·3µ™+ œU.gHçuÒÂÌVé–ò|W‰˜”¬€
¡À8(ÅHÚl†Je­¦äZVì–—§3@j:æM…
fcÎ|P Ÿ¹Ó¢Ó!ªz©ò´iİ¾—çJkñÒi•E0+Ú#UmS¡µ†VıWôÍ’ûP²è±êème½xgh=3]lsp9˜´	ßáğåˆ¾Ïá W¦’¾ÙL^5J
Ö~ÉîDPŠ½AÅğ.z$ü"*>	¿Y¯DÄÿ2ı’Tëêq¢˜­ óTóêÔ)mı?–zI\_ªõQÅ•ªoèÕ­ŠC/ö'ëõ–òYaÖÉ²…Y€|øäİ}‰ÈÊ…¾ãp=ëÃ.ÓÆâÁö-ªÍ°Fd&¹x;¢lÒ	KaÇ“(å°3Oíå,´Û°–ˆßÔRhO_ïÑÆ9ÎÑ¡Ù¢©bÃ¿u>7Üã|ãİÀ¿hÀÅÅTŒ¸¸YCŞ$ÇesåMfa~Iî3K³Mr‡Yš{’›ËÊL”ÜWfóRPŞ¨”/ÎRÉ¢Ú…9+ 1«;ìlÖ‚Â|uñä‚Vº–}\AÉÔ¶r%]SÛÄ•TNmÿVÑ<bcœ+ Ùb\ÒCJs\RGÊõ¸ •*£BÆ³ ÊûZùÜqé…b÷ƒxÅ×ÑGôòI~>Gßå{$Ù/3±³BO&‡ğ÷¯ıµ!Úî°û±ïQÔPÆÇ®8®NìÄæ‹¥ƒ<loğ÷§]ş‘†âø?&ŸßÀtØØÁ‘cu¢Èd¹;Pa€Ä­Ùâ;DÏ´íù1ı6Ìó7ÜAÎ8¾GŒ´Âs½†ñ  Ñ3»iÈéå{uJ!0É¹i± ¸#6ªäŠá{œw1äÄü—˜JbÜÉLìI°¥¹Ì.ˆ6x»¾‹¹è«¸;O4ê:p“ëKÄè9~±˜ï£ı;dEÚ8d%8Ó=†|J†˜hó'ŒÁîe6êäÑ&dÏ¤‚/Í03Å›°ô°¡Š×ô¯S©Ãbå*‰
B”˜‹Ê9–9IQ8ØEÄYŞ4S2÷7Ö½¿£!EgÃ±E¥·IÒ (Y'š…ñû~Å3‰X“ÂŞ-YlÓ87“¢ÇJ_IÑn&6ûä’Nñ†ğd¥Ü|òl·±yHŠÌĞà¡ÁXm6½fûu‘Åš7ê!¶‰¹¥\e­ß¢o¼÷Åä‡Òlú·á÷¤C6ür¥‹}é÷Íı‡»*øø×ñ†,«{´vè±MÒ¦añ`‹EŒ\¼6ÄÀÈgsÆ¥z¹Ù#ÏÉÈÇ‰	°Ò>{Ù&^ÇÚÁv{ÜyÑwV[Òƒmrù[fì~q~ıå/ö‘ŒÏ–ëåä¥¢bI>]Ã^f«™ZX™y{:ƒÜM~ó¾}<"Ïpã>K—ôù#b‚¿¢ó–ºû=v>9n-©öQ¹ÒXÖ79ÓÒv®c#dŞj©ª\*M­í2°•ùÚ÷>­Øú²%nQŠ‰P¬{Ê±¡Z=<@dwô:6qáé|Úk¥†6~;ò¬×ÒÂ—öMRvbtìÛŒ^‚WGÒ­˜ÍÌ“ZAèy¥FX™IJ(U{6ŒÙd5È÷ÜtyZÏM—ò¼&óÓxMæòz½^Ÿ¦×ëµ¯Å‰¼ò¼V'òZ)ğZ6«½¯ÓôĞ+ğ:±—ò}hÖ…f[ÆëMç…ª­ö.ËC§Õ®D/ÍËXÄÊXÉp
|§+B*¯S]İ¡”\‚SÅ¥èl	jõ´i/(«y¢üŸ°';±)’«ôä©=(×s]H•Õ‹ÓÊ¯¨µdtSJ(­'.|JëŞ‰6p©àë'®çFëìÍG´›-,W £]•Aº%'µ‚Ç%‰“6×U;¤ÈA@D˜&'Êr\v‹Cz`ş¤ }à™ãÈmu›Nïà½ˆOQò6òòÓONÒÉ·Å¾,I^éÎŒäù½‡ŠYûóXDË"xuÚÒãZÇC5Bò¬‘êÑtaXå˜>n¤½óÜ*}ÔL“ÄìÂê³F*oç{;\!‹ŠèğÖÂKKØ6òÛ¾_„†¾,#\h@OT²‚i*ZÁ€4ÍCÉ¹Ô¼ğ2ıä–âS%;"1©‹é6§šÑLgœÑE!º(¶p>—G5¬=\½rõ¶‚b
9E´u4SÈıAD_Ä5…•G’úÖŸJªwX*¢,!Âê3ymÿ²Ä˜Õ²7PÀŠ”ìrÜšØ¶mpŞåØ+\ÏsT°zÊ¢tQ…£pJihY½,Íü@ú¨ı*Ö3í'wË§Sd•ŒmªÊÕA§ÌùtfÙç¤îÀĞµ$	ÿŒ1L’¸OŠV’†{fà'I¦îÉÃ P£9(ªÏãøu•ÒÑPÅÓ‡	ëZLAœ4VX5(i¬·jRª¾j0Î‚÷tâï4#Èt¸ûu-U5²üB‰>v0ÆÅ»7»acj[îlÁ¹ªW#\¨§v‚JUp’,ù#Ô
x2‹E¾(âQ=JPF3Š¨ª
rt¢ˆBbk8‹>ô:6æiÕ­m¬Ç¯‹\r_Y !¦×@&øÄ^?jpE,¢7³#TÅô ’®¸’rA ¹ÈÅ`Şã|}êcômYÍÇEã«BiŒ+6X…˜œ›â>|1b“½F?cÏ¯îm·t=3T0XÚ^œçY{8Ì-M-G÷…uàCöíCBd¼?ğşÀû“ zºŞÔÀ•»dW®`;5l95c=w8WèëÊe'%]¨ìL· üªZ>='sS	¥`ZÇWo¨{\BMåOÅfDz0®c™#kI¦5
d«q®<*ë#½®"U¢s„šŞ›P-Û°Î(*L½)¹
õjÈDûÓ²µS7p³ÕÚAæœóº¿ÇmàÔ¶z`¯ƒ½Òè¹>	ë³4{6>«ÅÜ4W†Ægpã–àr,ÂMOÁq&f†5µÌÅD7HãJ×Ù­U‚òÕh²ªIï¼¯Şm¨jtò³Õ»óû³D}YíÌ´kL,¥pLzŠ‹r|DkœÀHıºÉQê¥K^n.8lŒs+¤5Ç™MÒ0XJ¥ãIÏç–êªÖyúQ³MœJSsÕnºGÏ5„ÂMI±}ú“†Ì¢z®àä0Š³Z¬æ3ÎKä<·ò$6Wà¤Ÿš„xk¬ßÏ7Wî–+xÚOÏsŞz;QÑjçĞ{gEe«‚¤×ĞŠú½:FÉEÆ¢âU©«÷‹èªM8 ‡Şx+Ï.À,+íˆóªâ¯LÑT•¯xƒªˆ¦:ìM™»yuÌÙ3•y_¾ˆµÊg&Øi×¯oQÖ4¾~›«ˆ´ª6õË]…âV¹V.{ÒUõH|ù«°Šªf	³¯EU³òËa…$U%QËØ
#Ós>Æ ›ëQöZa§¯¶ÓWóµ¹wi7¼ş©v·ô»¥ÈnÁ•t0p]£¬T½sp¤Q»ÅÛq2±mÛ¶mÛÛÖ$™Ø¶mÛ¶móÎœï|çŞûO¯µ~ëÙÕUoÕ[½wW¿Õè? ­WÆ¼±z›‚],ÉÈ£’óa-‚ÏSßÓqÊt>F»O¹ñzúewÌpÚUä¾fê Äéï*!b#ù¥Ô3ái·åĞkÔÈÑluñ(ı|k×7Y'Äêå÷Êy¿ÇÜqo6±¯X6qq½úÑulj„¿wuRj^erm©_:ñ¹\½Ğò‘û?,ŠÁ± $ù‘¾•‚G$@˜©ŒEÅ8¢¦Kce‡-RÅ‚ÃK±ª	ñ¯(awKci†=HÄƒ§¬9ZÖ$$Û1–	uc¤wqÁæ8³¯fÖx”+¹˜_÷¥*ÅÂ<ÕŠ¶ Æ×†¼¦¢ß*Zõ[O@}–ªÖa¨X>Œ;SHTQ)GÙæéÄ@çrê–‰o—{$hYŠ2rÆ‡½êRŒË”—ƒÙÿ²Ë¯wu3ñäã#ATo+‘Ê|Å>lOoÌÖá¦¬}asB»A¯>)&%‚çuŠÿMò0QÇË4Ã,ÜÇ×Sm}¼}}Ô=$\»yí"®¯ŸĞêõáÛrËšåSãõÌ¥=eÛ½Ûkì ½1Aîe¥Âï~RÆ›£9_f«"tr“¯ƒçèìˆ® ¹õBôfàô ÷8_:ä@/4åÒ¿vp	øØ]Öh¢—ğ$‚Sïï[ ~b€x¬@ÈæÁşk€jáo!jáu[ŸvI.‚PƒY\!ß[îÙíô»º[ŸSøšy–fÛ>Ç“Ú
¥”Òêu:½¾m»èlŠ°“úù~¸£×§¾›ûÅ½tÂ–ù7½ÉriõÄÜ|L†fd¨ãÛmwmn|½İî‡Êçˆû}5 tí×²nSìR»u…õŞÂ™ºÑÓEq=«ÌÃc²(=·¬#Ç¹†Ñ6gkÚg¢T+‡û—ÅebÁ+<šoF%N¡_x´ÛUJÆÏ›ò˜¢Ó«G¼îímiƒŒ˜
Ğ8bÆûëĞÍ9z€F¬ÿ#IÓ;^Wbi@m:	ÒHmàQî~ôp­Ş¯Ü~ós½áWnKÜgëÎJ¥Ö]z¡ ¾a^ÌzùA¾0o¦çPüıÀ?@9wr€¾iÄaÿğY•ÀÌ;9^ğN€/Í€”!mê¸ßüoçæLoqøl'¼L€sÕ@4¸=KN×Š!Ó5Ä…VÅê£?.à` à^x^¸¶âÔ‚øCD Àº^’# ¿ÂYÿbÌ’K×‹@ÀÃ¿{‰N!åá;¸=,Í‘ûvú¥áá<ìG‚„ñ:AÈÿ‰NTI3O«£šg»ŞsBÒPÂ¨gW½Ìˆ”°Vı\lRõ5!õ¦ßß0«rQ_u<hbIØA3ø?$9æŸ¨¡x¹b:vÄ®oTÃp¸¶P‹ûğ¶éLş Ş 9øIÈİ¶
—IğO¨¦á$çDõŸ±CÚêq$â…âQâcIªìú$H*ı/«ŠW	g%‘$”¤"Æü'cÈ€úÄØ¢_4E0œ&”„Ò”â°šR#JBÉHõ‰6™„ÒPÿR6¿tY6£Îc"£ÈˆÃe4”°†P?RÈã&	”ğÈ›à|½oìc‡@á9y×sP%¾É'Á9„$»D»àœl8N¼m¬sBŸò,ÓÏ¿×Ë“y$Up¨òïõJı9±à¤.İ³±ø£FfİU-¶3ğ •¤†ÀCu[UàîG¼úÿ©r°Qj	=Á»pjL!Dà0LG‰_D"ÑXH„¯H„±"$Bt”ˆs„=äüº5ÅiD/}¡_Ä/ßğÿy”ŞËiŸ-ş›jLms©pÊA"Ş¡>­"Š9¥jbs§ŞOG·ì`SM*M>–İjy¿	O …Dî/Q¯İ”"-ƒË‚µÙã[…èı{Gÿß;ĞûÎG†ƒú!XüŞ‘§œ^^f	Ô§s”yí$EË–ibRv†ÀU~şñB¸¡§DƒÃ½%{Yä{¢}Œè,è,M°®ÛICÕ{0Á{À¬‘óã3všãƒã€™ ‡ñÂbÄlÄL€ä0SÃËrîh®O”:åû—Ï{KÚ3m6ò>Ú‰È—nÒJv˜"â±[VºlT7·ƒ75á#pºD—ÀYQÄÆÿy0%zØÙÀèB=¹xÏ×/¯§÷zÊLîPS©tx¼›¸töåDGôÉ<¯¾vÒØ.ÿ‹ÁİaB/ ÕCyá§ü/Á'àzô8˜ÃkV¤Xœœ$te¸„p@¢ã¥D„¨aaĞÂ–³ûr˜œ(Õá„y±™‡PM÷ÇÜbÛ‚_¿€Õ}RjÀ%Ú`İÕ2!‰!0TÂTB™Á9cÛßİt¶ (a,”G^dªp/ğ‘0^¨?'èD(//‰Ü¬ín.Âeˆ9»HÇ{Áæİ¬º¶:¼Ï8kĞkN~:Ââ¯ÒŸè³¸È‹¿RwŒBğ½@GÃáÀíÙ#„bßqìf+ãÀë
øCë$ü°‡…á¼Ü¶u5T4Ú—€WüÍÒmös'
Ç"Ş‰óp£G¹ hÍÅ.aë•é€Ãİ›©nkCëWAKN¬×ğUeĞ¶6
~²@k™]d~/Ğ8Ëíã•-Ê6˜pÌÕD™HCVà±Úï2•´Å+pİ±·A)G…6È.}Ÿº'q+J—µFcÿ™Š^ä‹Íªó«ëUj]0Ñ"’"!‚^ugøüv¿ƒ=ÉR6ôÈhù¹„ß¢Œ¥ÒˆwfúmÄÍ˜ûàSèsæZ»3áÔ7Ìw<Ü\Í@:‹"ˆŞ[©hˆ  à– ö1knŒìEbß“ˆúFb>¸I4´/jÖ*êœ‚!ë±ºH'>ù¯øĞƒr™´l:ßySãø–_:LD5L'â¹İÂ_ø³3®ÓonåOMå>¸3#ùí~iãÿÉ@óÖæ§¸§ñFËƒ£¸ÇešM|•ë™@g2ÿ’ãXlá+M×ôAm)¨}‘ºàHÃx]¡e"T‹/	Kˆ2‚‘W_3E öÇ €$»Id¾É}‡ğQÃc1óáe-õÙªùğö9„,ƒ£È4ÛŞ{™ÊòBÍ´¯'rª³/9’yn¶¡ò¾°ãÿ|¾ı³hú
ÔK¯A·¦OKâ×Cb)zç$:¸ÏäÓpUJ¸¸Øı=rÁ{
ËEh„‡dvæåºü»è#œ{*‰àddBÃH­
‚_*5(àğÊ/+qÉ#Kß¼èY|*#;)0·ÅKë‹pcêŸÒÌ\WÃcï—êjë‹Y_ãî„»”âüSp• Xˆˆ‘ş·‡,é˜`Ş— !…s¥ ×ÛÎŒDb‰ÂÒÎ-ó<%”5Ws±‚€ P8$	^(œÂ'‰é)it1°U³y~üëŞ†]…ä'–VNTyãº^n€:ˆNf+ÖåŒ¬=ëZƒ¿ÎDa'ÎF¯qZÀ00ŞxÄäàØUœs÷
{‡ëÔÇæ\]ûÍ…Âoİ”fj0]y õ×7¯,½ë‘%Z§èÃ;/Uƒ‡-&/W…óQà±	NlôX˜¨«ÛR£x¼pD!÷¬%½È°·ë›
"¶r©ğ¨Tõ>¾Õ‹%~Èêë; QõçT…åßI	çre€7kíqK8W]KŒôíšßL]úUZ¬›âìCuGÖØOºŒ÷¿ &ûèÓÈõİ²‰{Ò=¿† Í"Ÿ´‰Ğ÷´íŞgfá{
&'§Şèv[)ÈëšûÔ|ÏW@î†teI6£ !f`Šlùıô`à®«[7â#Œ˜½e°XâÚÊ|¸İNÃç“æ¿S‰ÿˆĞAŞî¯($Õ?[È[ï_!¯Á+:Š4;^n(¨°)M„4	ÕİD oÂó²[Çƒ®gvâš´ìaæ>å”½0í¢°™ULÊ¹COnÓBm_uìW±iè—lªø™ñøæ•“£i;3v¬^<Iï¸]QD³7mPë„5ÑKr·|¸´@ãùÉ6|ê;ò|Ag¬~ùâş,«ºvÿC¥"âûÎŒS½°,}?}¬Í9@Ö®_ĞÁkGĞ.¾lA´µDòƒ–’øÁ8]!$’lİâÏ=€^Ö.5n¢3¶ù	<>ï„šœá¸›e°¿Çm±´=Àî—"EÏT`y‰jŞŒ+²t\¹Eš¥‰ˆ¦½Fi¼êÊt”‡y†¥	¯x”åÅ`˜©gÀÑe–&u´,ö`fWi½TAÿèQ›±õ#j|Vò˜™R¿o=<Å¢Ø+Mİ1}(³7….#ÃMo[ÕQV=‰ºÒÇ)b±¥eQx8Dˆ¥ğbÃÎ–+÷70?|e~ÙìCo9^3ZU.Á5|‰Û˜…!i¶’%°I‹ßTƒ¢ÏAÉUh^‰İ5È/Úõ–†ë­	*³Ôq6ºŸ/~‘“FÏu°sd:­4â­À’Á}ùp<¸=R]ÕÒ\ıI—a¾¦§xTm2À‹‹Hò“ô?’hÕOÍmÛ<£´ÆkIŒ8Âuê‚rÆ1“iâ”¯63Î|í5‡Tğ™_°$VP:¶±\tS.#û}€¹>TæÃ;fq©ˆ¥Û˜°â5­Fã
á%xÍl¢	wD­gıUÒğ	TÊıUô	”ÀvÈÛaıñü®
œ;Çköø“`ÀWÈ»|şòˆc»|Y»àÄ4í²æ¿äËÖxmÊ³Ğtü-!ùPé»Û¡…S†mØSùñÊ\ùi&ËTÜÉ*fÈO•ãPëƒ;èlLcÇd_‚®ƒ7Ë>Ü€ÚdH©\Şa]êcn—w•¹ÚŠ¦óQ}dûÒåÓáÂ<ÔŸÏC²ì°i¨4Tfÿ˜Ù)Nšx)%ØX2¯6>áË°¥^”¥^˜ @„ ¿ †J|ÏĞ€Ù°ä3-GÅ!ÈåÌµG—-”d¿¨³“‚ıê–Bú1½£Å,ß’½ÁÖÏğâ¡ ªgïlÚ…rğ[Ì%àÃ§
Œ»Ã¶	‹¯}>zİóva|ªZSÅo¯lïU‹­`…Ù2Å)biåŸ>—Ôˆ-° ç]/ûè–I¾ú«°&{å6<LF>ücÂ)~"„ñŒºYä·J˜kü¨ˆD•äZ"bĞ±³ÏQóFÙîyV„!‘¿¹Ö.lMr5¡€A}ú<lŠÆh3¦´&=mF«|pÅ6¯èŠó‚svÖ6…ãzSÇsU¥öß”ô7Yõw©’CS(ò“£!h×'‡/Eå"Ç30.|$A»Íäğ·)ƒS ö‘£émÖª@HÌİÍÔğ¯ú»Áéï
s1rø*.r8½+Rä #Üäh‰òÔØR;œŠlıà}Ÿ_?=šÂLÁ'‡`0X(-"ĞÜÕØ³RHrDTäöæ¹”;­™ÃÍ«î©4FİI`Ç Ió€g[4Dl(‡«1è°N,Ø(zä`Ï
uQŞ»çºs:EÉZFAxzCpbÎ¾š‚›¸–&
öÆ˜™‡Ì ˆD>8ò7š¹\Ck+.cšuyK”k/¬š·³ökŠgëe÷†Qõ0:¬Ãgt›<BsôŠ'ƒİ{àLUıR6Ó{¶R&ĞÍşŠ»²óí^/yG1µK?*]\×dˆè.Š_|V¬A›wB~RÚø:ö;èµ}J_û9‹a³>ÁÚÖå¤ÀÖ«ƒ¸¬j¥p[SÂ”I,d9 ŸU‡¦6ƒÁ÷F3Œ?räsk†K¾ÖÉ¹d)É[³•Å§Ã„<ãV=Î5{j;‘÷ÙĞ‚ç6··0íåÄgæs–ôä8åMıS±šß+£úÉœ}§°¢'©nª.ïF§™"Ú²-p³P¾ˆcÕfIıq'JW!ñR÷k–¦¯~ıA+âüwØFÊo¥ş!²A7¦+Î?R´ıøÁÇ£Ï3>~9Eqàc¯ñğ·¦Xd
ğdœĞ6Ì
ù_¦-sX1D’Ïv¦„°§
TŞß´Q³bè×÷}‰f‚)y²(éRSçYhÍ	ÇâR|ßåA`¯õk'·¼G¨K«\lyx2høìÏn·v,BX?áÉ*pÙİ¼`@O†)F1OŒÆÎ\(ÁE³ìS†•XËF5?:·®ğYeğÅOAÍ×=V}V*1Ä™`b#ãûw_ŠÜeìmòr÷ŒZÁûÚ¥Ê[vòñæ»\£êÄ¿
øä&x‡alüînOõNÊãpLs)Pš€Ó˜¸úİ`ùí'FÅ/^[õğñy1¼“ÛóÍ£„ö8ÑÍ¼ø½Êcp£#>`Â½C_µŞ^Şáîvz¼·åññcÍ4l­eºo”O~Ÿc†„rç³;”L„¯/GÈ <Šâ¦2q[.ëÄÁv<:©¦sîb„!O~–¡…>ß1éå+úaU£†FŒ¢+‡Q`¨#a‚;’‹©ÚQˆãs£‰¼gs·%G‰&Û(‡*›éÂ?€S,~uåÿ:¨c‹¢ZVÛk?>i[ä÷¹€Ä]9ÆI¢´Œ|SÃx:`TWÍSú’¾wÇQöO]YÉ_Ã©-£³Dğ[ËÏ·°¥Eô
U‚Bäöw>THhß
Ì•0ÉÇm_ËKåàï£z¥[ãµ}¦X"
¿˜+ñzàÿæ¸¥êädu†•Ç¾ÖÏùõLs!9ĞÊG}¾·nªÇ3¦Pv1Ìbuøû„Ep]^ƒíîM*¬[.4‡RDö¨L¹€ïiË³ø—~-ÏFYwİÎc‘—]ÏĞwØê(ı–ó­ı£^û–­«—Ry+!4BçÆÆ5·›·€©á:Ó%ß¥İŠo6{lY[èôCÃÄºìR‘6œlõ¡™µëG*ó´Yíà÷$éÀFÃTµ´<©µS‚ıà ï†Ë¼©iŸãwëánFRpNdY÷@c—–4{tAÀµZµö=Åa;t“:Îñ=ú•b£¬rGøÑ}ğ!¬5)‘m¢| ‡ä…ÁÎz–÷^6’Í”½°%ãy¡©²î÷¦ª0es££7
‘›:-ŸÃöâû“6DäpõInC”üús©	¦Ô%sCšôî*ğíßM?é|«z0Ø¡¤ĞëÂ!ïÖ^—Ò¸«:í¡Š`ämE·>ZCél&$^Í8«Çóîz3ÂÓ•°ŒœŸ5ç.K•–7‹¤=FÚ)bÄ^“e.6˜úÚH4²¶Fƒ=—®f;]`aàr²íÙÈo-Ñ/ÑŠ3£=Æ“ê>†8§†xÛ/èt|i›§ÙL'¦ê}Dï8kí1y2r3õ—¾·¡QÇ¥>Låa'î;H¨¨„^s5EàÏõ;yLı²8iı¹¥RšàåbEíHò^IwóÏ°m²Úyz·ÖøÛ[ûT5‰ñß5wœ?+{İ=¿X¶™NKïëXš·¡ìİÎµÒ5+®PîwÈ=Pèäñ¤q+K÷ÓA(äö<ôpÂôb8ÑK ¾· ‘ôâÈ|şã¾öPˆ{Ò€°öâ«„Iù?£„÷}YU¶Í»dˆæ’Af@7¨NIç@pùÂœæòßÈFwl`!f×Òƒ]Ôv¤öóßğüéãB@Ú‚	(HÈ"J”	ãÈ³÷ç±~øÛçÚ§õt§ø³÷ÖŞ=ÑlÍkğ¸ít´[1¥„ôøîÔ\“büâKßñ?âç^e_‘¼yô©‚ƒ;_é±Í÷Ï\ÎÛÄŞû½ì`ĞÔÖ1tìîÒ#Ö9~Ğ‡JÖˆÄ'Ñ×XTe4rÜ•Â˜‹ÛÑõc¬Ãv¶àDéÀZÆ ?µ
ë×ƒÏ;—£7{ã¦!½9ùN÷¼`û~ÔçuÔ·Í‹·öŞ,èÁl nÌ2¬š<é¥ç…‘¸[–U÷ä«ãIù;óaÅ^‹cæÈvå‹İ…Š+~g,Ïí~³Aö0&mÊÊ¡¶x7µ ¬UÍ²DcÖ¶fVÅ,Ê F÷®è²¡j1Ü›DnˆĞDÇTîòş´ïC¥°hß¶ÕdDâØå€-á©UædKš¦t‹Áxe‹½5§Kyö&§™îª#“„mqœ,²üyŠ~o8#`ÏÊXÔX¸“%Rl”ï W\™C4Ş1¶²øP1U¿n›aÚé}0\¿´«ÊŸïÉ‘½RÈ0é×ZxÀmÔ2§—YÖ¿ƒ}}N'/Òa´^ÖÙºĞp¬¦2‡"YF,"Øó‚9$YA'’1³Fıl®ÙÒËbƒq1ÔF\HLÈrÙÌ™>*;Ü7Ä;M}cã´ÏšÍñŸgºÑzçŞ±Õï^67]±ÛÁ½ÁÆ…éİ!ıRÑïòİDX=}l=Şˆ@Ÿ²0Ô¥kîPé‡!5óÓ¾FÆ³ô¢p”iÈGw<×Ù¨óÛ-ìvü^kâ>¨ÁÌN¥(üÒ_J’¼[y¤û­Üy›Ã¾^1^Šı–’Ãöşù3ëû6tõy[üóK>n¶Üƒ¹¸jÏRußHíC^ÈCªš¨åEÛñOF0`à0¬ì;~—ĞvÖ6-o/ÆsYoZ6Ã#=Ùb¸™s‚ëèüáÂn„da¢î8Xò$¤Q»}ïÎ[š=H^ó>@·€úïi4Ÿì´¯³)ß5ÃÊ›Q?Z¯ËĞ„0Ê ¸i3_ÃÇ)ßİ‰Šä·Ø¬¬àİ˜—Üm,w-­Ç «QrØ^ê¨u•rÓí+iŞu)ô¢@ó›+T"µMa¤ÁhÌg"h7åÂêÅ´]h{a²Ã“±CXÉÈ`Û{èèeœO×4ÅB-sÍñ×G?Óuy¦ñS_İZÇ›¶²ß±ßÑ’ö‚µáQiö©•ù„¼H‚ğs|T©·¸øîü.ïÔ€FvœVÎ¦şsL_äwjj²4Ó-~ÒñL!‹Ï<Cäjuu`Ë}9´EtQ#š´/mVOr·'|¢÷Çl‹c¦ZlÜı×P)ğ:+@éF^:ˆ£^Ÿl³ â¨Şa¸¦ç‹8µXZ­:í~•ß5…Yp.îLÖú(Ë­@J¤ë—³22Á$»¥5K2_T§Û$EOv_`~uÙvxïxÅU_EL~´ºD<ïqã†ëŸ—S§ó•|høåc04¬—S;ç}Üp`ê+Ÿc«™ïÓµRƒj§ı}‹ŞöÓŒ-ÜUÂ]A7ª,üä^(T{¤Ê\Œóèù}^ƒaÎÎº>1cOI©”—ø¯YMC	àgó)®òrEO,œ@ZxQ„r—ş˜vÛÜ}<]š?Zá€h÷§èÅùrÊæ²J‘LAH¸’ÈhÛï´–wó¬½”ƒÁ%yÕ-BÍGTàRŠOÄ*¼·#ÔŞÕÙû¾R: d|F¬5^rÅœ­h_PXñì!R¾}Ü`í¨Ü ¨²‚ŒÕÂéÒ¡çígö$îîÛ°<–cÚßæÂĞòåª÷Î;èê/Hî ĞìÃSl©È¶ˆ¡1å8j]E$ô\)Ø`é!y5ïğ7oŸe=Ÿ?·yÇÒ¡5Á†İ´Ô¢©òÇ
Ôû¤41Şqìe¯¦¼>fº…TOàx­m[ÔººE;\ìC¯8p¡Ëïtµ¶QœÀ©*7­“¶6]«“Ì;å®EºP$Ì®À&7});#}·‹¹Iº}åÇLS*œ³\ÑÕtõ(SÆ5-øªŞ"\-èHÇ„’/æ?Ë¥AÆ¡5µ]%¿‘™Bu±âÄ±Mšñ¹‰<…ùSÆSÃ%ú²çÒ"¤Éå×·Ë×0¿•/…‚¤BÀ¹³f5ÃĞxš5z‹z9’¼Ò´Ìu7ëEº4NÒæ¶½Â·[B9‹å"8_å©ÉËrR‡IÕNKŸfÃîı˜—óˆi‘½á
\ïÏúÿPyenı,qµ¢œ|Èp\l­ª"Ås¢×’zÏ]Ÿ°
r˜Sã†X<€“zàÄŞ ­˜[ÅZtL)(e=Û£Ãã¶RéæÂj²M*
m*_»úrJ¬û‹ĞFLâ®WJºæ)9
U­ƒe‹˜tXFñ®ŸC+æoªdqà“@õzÜ­ şûrà;ğbh(Ş—÷ªÃ“¯™›
X{P5wJö9Ì™{
ú§5ì!Ô#®Ù<šB´Ş"qîsÀø³0}]¿Ì‚5t¢	l ïÏ0nä‘Ø'È2‹bY#uY‹Â§}Â^}‘ÔÌP¦û#îêä<Åzw¶=ƒWØLË±iÙiäÊ|§h:Ûära )8kãÚƒÖ¬_eÆùÓÅEVT_ÔEjøƒ²+¹FëgZe~uGà[]!¿a|LcÆ!â½ydA‹çSJº,±ÚDı»&n«ÖÊ‚±¹Fóöñë¼ºÈÒtÚgCî	_’_£J‘u1¥^%aN°VJ*ø©<JOFb)1JÇá¦ïÏŞg¾C¸šÆï-ÚîÀ*øPĞ<Y°:¦k -sd6Æ×)y½$|*¯@z<ÙêúÖ©˜2­ÆµìÜ¨7™ò<°çê¾gëFäpéú›ŒLUEĞAy¹¦+VûƒĞhv‘9aö'sº1sAõÅèS™yNjGıâÏ|ggw3»ş`n?t³¢©«ZTË™Šiß‡ÍÏr³´-]8K»ÎõîUJÓ®í.ùHaû+ï˜£¢´_œììíì‰†ÑJ‡ü™ûû¡Ÿ³œ\D«@pÅóÙ]úfmõNúåjıs]Ó¢ß…‡ªÄÂ_T47Œƒ	¢ĞÈ&^OQ.ÂÕ%áòä^çÏNDUcæ^†Oü0‹RÓØ­iFG³7YÇW*¼X{sqÜ¤CËß ìî`/1_Ù;®Å0ìª­È>> œ*–éé~ £÷ôòı÷™M;2
3§´¶Û›g‡Ûi¸êD¿É¡e'eçrLÏ®ëwk¯¶E
DÈ"¾òe©t-ËÃ¨#‹ô} „ç×üç:šGJiJëas‡µ1éëÕÙ›Ón‚aßÏã
i !µÓ}4B\b•ª¼R¡´ı‘?7S;ÂšÖå½ˆì ›†ó&´è’rF#1iı¢óÂôÎWå::ùõ~mÿ}Do4^V²HK®¡•¨ùNÚ6e²À\>}Ãô™@­cæéTâ ¢š{&ÖÁM\<‡”ßsûñ?U\ÓÛĞ×±ïU2S†5ZF22dãrwæd{€x÷h#×ÜÅi•1âì^Œ&!B°/:IV$œtVÀ(úOİÓãt=5hB¨9Öİ7Mçî¸ §·³·‹¯ÀËB„	qø}²ÛÈpi#ó{’=ûì<coé.¼:™¤ü¢]‰8eÁŞqíOÇÕ’«@½"8öÉJ\íÕ;·«¯“¶õ*ëÄVB¾R–Ïş¤ÈMCìÌ²ØZå™¿\óL‡öÏX¥hÅ|³¬â~à712ÔU¼„Sö‰…Gg[}Ñ„9îKsŞo9“v¬Uc#ø×^JÑÖÂ…ãœÔéW]Ìò»ûy½¨¿'Òì1¤&ı«ˆ	Â{¢ä‘v²†aşreqÙµóLv¼	4$ş´MÛğı‘­Ÿ^ÍÈİƒYÄ¨#sÃ7Ùò¨W„°À¸ˆ9S[	UKZÕ1B_´Ÿº3¨Í¨ûnMP½ç]‡ÑJ­„W»»²bTÄe·]fü×~.>â”#´K:Q5¨âæ©c•zİºé…O:ÏzlúN5.t²ğt1õm=v¥Òñòx1¯gg3Wq ÔÛ°–2¤Cç»–œ×†Æj“ïı†“ñÍ&ôS©o?u™B[}×7n÷£ø(²ãÿÜ—ÛzÇÚ´HÉ*ûeØ‰có>!IÇ~u†ìÛ¿m¶Œîø,—¼-ôn2¢¼x•åÙÏâá}Ëzëim˜XÜÉĞÓ÷ ;js[¸ÖaN„DHFÑ ']JEÉÿÊ‘=6²^/&ïGg==11¢Ö9Š|™+§Ã‹—sFsºÇ~gw²ô®‹ç*OkM™³’=k¼Gò+\hÍ=Qî¢;?ğxDÍö3ó6UÜÎ§é\> ¼ºZòV-ˆo—55U€ö±»ú´¹¨­ïñµ{V]\²1×RmúÜd’[¾²h¦Õ>/õ­jãÀ&™ŸàÑº\OmU¸cÔ9};şAîY­±¦WöÌl&5õr\ê¯Òa[}U#jbÂ^Öæ:o¢ €2ñºú´±wŒ±«ÂÕ?œ¢Ş·=ñ6MºläS×ì@§–ë¸İÀõún 9îãƒœv~–M»rTÁ1µk\ÀÃ§Z9İ	ËrÚc†WêwÙtº¼Â7ñiÍ^Ğ¹¯izK Î.B“7òÛkHLòŠµÃ[
jà|[ºsæA¢V¹ôRgÛl¸¬òQu9vÙğA¨¿šÚô‹‡b›’ÂG¯HhÄ¥O>~¤Í[„¼Îò6:¥st_—~VÙhĞV¶Ù×¦"÷ÜÇ‚9*	*TÈ½+Ñp„tK8.
nó2Ù=Ôù³Úù¡£Ä¯¯õ¦›‡ó!_F5\í÷\åõaØãp1Â/ÁŒêòõİ{ÛåèÆí†÷ZÜj³O•C›2zKA-¸fÙòÒDe¨ÄÖ¬`ù}·7(÷}f¶ëEGBe¹`n¯¡XR?"PJ¡r…*õ:ëáÎó&¸	–X€
®áÙ7¬ªLÇ»¬7^öXÇãbëˆõr}ûvˆ '—Óo(&¯0’İ—lfQóeÔ¾„w®Ÿd(%‹¢Z;2²	v‰ò…‹-{æê…ú€x8®›ímFù„_ı¼ì·‘@NMú6à™Œ¯sÆ\ÿ.Ö2X²StºG¼J%õµ…,ôX‚a {çñ»Û 'ĞÙw›g>¸Óú5ÏŸBß9=¨à‹7eá6:Æê½ı[>`à`ä‹Bü);6ø½˜cVï%/ªd¸ç¬ùIº—s«Ì‹?\Ö^/¾:ŞÓˆ¸Ãå†ßªò f»È'–ŒÇÒÓ;›	¦èñG6Üuéôƒ`¿ÍşÙ‹vÅvÓ¬Nd\şb’ëuCê‡$| Áû¡ÍË°ø;†u|ÍWsçş‘;(÷Øö
¼‘Â˜ó¬Ö>—åÎ»Œ®‹Kkëè§x)C4ÉÂX;~Êuúİğ¹ËrxcU§`dQyáá?Øò-	¯MéL]Òú@ok^z»Ÿ}ÆÎá*3¿nT4bæÇÑ=·vaX°–QãMl4N²,„ïqå´æëa£ÆØ,©¢x$BØÌ©lşöµæ6µqKÖ Aõ°è°ãdÌ“=èŒL¡êİm²$qVGdãkg^ãë†ÀšõšÂšµ%Q•EÇ¤ÌšÍÚLòL¡lz´Ø(d*‹MK˜d‡,ÖRZ@@ã{ˆ<Î?u˜wÅÎh\o-£ÅÌX‰¿­-c¹Sá[Ë-kü[Š
3·1ÔÅË»ıÙûˆ~p¯{ñõíâJp;}$É²¯;XÙ/ŞiP¾÷8]¾ìC¾ó‘¯L3Í„@¤.[4×Ä¾ê–wº´Ì¢®€ÿ{øB’èØŞÆ8¸ŸîĞP‹›@‰©s	ë=ò­ësnB™¯~&`gËG½/GVÊ†'»ô‘\{™lX£MWÿ%É:¯tø+“¾+!-ÒÎß:`ÌË¸k8ó°FšE¬c¤ËqL½;wNîÀPL×¦·H,ÍM¯À[h,İMñ¦ÃÊàX©î~Á’?å"Eg9ğ ¶i÷§hZ}ït¿ãl°PGĞÚ¨K?¶šÎ%O©šˆ­Ë–…c1# º{Õ–òç:øViº™;h¥ŞéÓ±¹rªñ³v¡t¢[8e×ÂµmtµjÌİyTG¾c£«GŒÜó™EõP)òág\Ê‰ÔCrz1sdñØı¶r;»iWÅãîu>tsTW|èÃ‹NîE’Ì5EW(7g(x¤~±íáøs*á¼)¤¼Èû´T‰|FŞçæ.`gC½”ñíkkşKêov“/mJŠYÿµ±@\‚EğÚg”€®Â’í>‡ªŒ@¥+F¤#èµ¶†6rbXŒˆ >“’P
¦}ŒäÏĞ ¾m GÈ_­ ¼æğŠ¨ö`kœkÔ•äHlŸEWŸIô@BWÄJxæaŠhÌhbïÍ:O&vãŠŸ b½"·¶GjaüY™!†kºEÃ"pŠÅÅ|ÌÈ “(©_UÄ[Hù[JÊ,µÂjÇ­ÑhÌnğ>“¨ÎÖçµOfÇ E±K$ 9¦Mò'ÁÃ/úâõK‘şN]ãÀn”rå ÜÃ¬:Ù‡›ÌÀ—TX˜š\bfÉ‘İ_4«MR‘Ø1}ú %œı¤ß‡`êS7”è˜‚`~¶´—LÜ*4{NÿŒwõ–æ’¹‹„G÷ıB¿Ä*Ä%Û÷¶#[1Š¥ãHà)n©$Ÿ"ÉX¥=e5õ•~;”©×Øp¼âÒê+ÓmO©Vkõ>«Ã,ÎİÉc3pĞ¾YÀ,Â§:ïë¸˜¸‹÷	sÜL3Í/'áN¡ÂÔó&»×vT&çg5˜/ªÂ9Ğ åMøÙ¶±íˆ'Õàm"í=fÙ¥sŒĞ¸^zu†¿ŠF«q5.7Vî¥ÌrÇ¦†:—Qg Úø(b™İ]\¥xUkÕ¢v²˜±G0Â¹?öNÂ89Ïk?™i=€µikã¹n¤¤¯}~ÕèªäƒÈr—êrlÕÖX\]•;	¯:>'Å+G;ÊäŠÕDÓ’ßQbZ"—‘\„Í¡¥•¬3(8W?âø,6ŞOÅ» }£{îlF¼_j¾Œ'½Ø‰¬ËE°†·¹øa#]õ˜²éåÜ}±í0M¿ÑB;k~ÚUnŸŸ”5M[âVo´õôçèØ}øõ²™ÔAÅ§ğP½\­àON_s¤»+ó4‰‰jùo{Ğ¬#kaó‘¸Æïl$CBHhA‰š7È|.„Å€R8„.(E\Ô¥lÄÅ¸¶ÿEå Š¸¿ÏT şoŠ ¼`¸=ù_ˆVÇY8ù!¢NĞO‚ıO€gQkÈÛO3!Ã@©‘‹,Qr!AE	(òÛu@EQ
‰)ò›şe`AD‹€íïƒrtEb9CJ9CÖ´Édä3·ÊJÄ°J‘IÇK0‘J€8àß(ÈD`T4#DJN„ÿA… "0J	 ÿ›(€–ˆìqNòï‡†ÿÂGá­ğ"ãx†»ÊB…ºêb¡mu³¿ŞÙêëRO¾ê"Àİğ²1üƒ¼ä¡a®>íÆº–óf××Xı:5§CX ¢à°pYÚ—ºsg‚ÄşEŞ.¿Ÿ:^›Ñå£ƒ(şÊ–ïÂä8Y~İ7­Ìî­SÄnæ8wZ0'ˆÂgä/Áyà<qš9.züKş'*¿õš¢Ñ<ˆ4
Ş{#ìÖ£ú-±,%t
Té@´Á9âx:üë²rÌÂx½-]yó˜í]
¨jDÖ.!Ñ¦—†°ŞÔ¡¨u18ï]Ò‡hæ³„ÒÎ	;u'ÁşGª¬.¡­qW¸ş30ı×=6ThC4ëy¼¢ÿk”ôÿuãIàÍpÿzóe[ájµ²_Ş©îÓUÛx<Òe‚²ÄZ`¡FZp¶ĞR˜Ï-”Jg…ıËšÿéìŠÚœq+İgÔx´*½kQzÃPA¨—ÖĞ5kÿÅôÎL˜\¦²õîâ_.ìş×¥PÃVbüwÊ§o>°L:»YˆnÇBŸ[ú‹µ«·'ĞìûÇ×c§ÄMÍ+ô‡•OÛ>ô\ßÿâEGô+Ø@çZó0]šk¾Ö0Á*Vú…‚±cºŞ?—
ˆJ	97Fù/Ç¿ıë´ªY •ş3ª÷‹ ,ŠY„ì÷Ä6ÿ‘‹ØP™4=J eàbP=ƒ±°Ôz ĞT¥¸T$R©ŞÄ®pÅæUDk+§‰Gs–nô'€9ñŒXn”Ö;°ÍŠh$¢3Ñ‰H>{€\em5ØÒLX.² y Ü¿ºNS€Ë–+n	ˆOÁ¯à›ûAu <Sü8)Bğ<€“,t(z? +À<`f¾d$BàZØó(uÃ~v	Îÿù|Ğº^§Ú(„¬¤‰ÓÀ±÷à_<¯ìóUƒ”‰fàè=øúùwA_£3Ãeı¬/úÙåa´’!øî¦%¦>°j÷‘:B­·ÁkïÅ[à!È¸z5‹ß?P«'ÁëLp*0PxP"	G¢Û†ëœB|š
6`ŞWËÑ7]E¸	Ò1,ÖáZ“
w‘İ6S½ÒÅ…°YXeìlñhÎ“œ"ñ[ÎJ_ÀÙÿ0ô7.kåW!"àÚÿ‡@ü†CØôğúßp¹¡=$áå$°Fık”*ÈÔ N +€åÏr¿ø¯Xôı×Ñ>Óæş™4¡š›aÈ5øÓø†Îc1S]`øñßÿ+hEü¢N°K)/ã6¶¢o“tìğò(ëZ¯ò‘ä+T¢2ZnÁ[?NÉ’Qõõ&‰ğE‚Ä^Ÿíâ$:KnÏñ%ñÇ_`Èb…)5´.øÿ$X+kJ…?ş&.º$ííögè±•ş&`$Ô»Àñ¨ÿ¬*£"&Ğh9–„š@ı\<4IE¸óÿÔÑr°íÑgÚÛ¬~{¾çÎ[È¹^7àneo;ÿkĞLâ cHq$à©8ğ= ÿk/ğöÑşN$6ôÿDeÛ|Góè¢•¿÷è*fH9dà¨[Öhü…­·å¯mØYG¡ßYø‘øÉˆì2ìæ÷'i†ö9\ëøÁ~À(ø+cmçõÀRizM€È"D2üGóHÿ_ÀV,ƒfP/±-ƒö‡2ƒâ_`	–.¦Œôø›h@0ã¹š$µ0áèµ0°f¦ÖÉ@ÓVó~¬ókÊÒ—xûöhBZØsk±KxZØñ±…!$¬$e„’›$ˆññPÿAş	±åA*¸…gª%·‰_J.±AÊ¬ˆö±AA88;œ­ôº¡uE$CroÌÓGXóÌ±§ş2&\ó@iÑda±§±2ÿw9{ÚÚ%q Q}‹ïÿ8€HòÍuô8İ/ş&û‡—9N¾qJá8ê[ş7á9
€ÿ{û> f@¶IÿŞÇ'°k0€OV/+Ÿ„VvÉU0fI +ÈÂRîÑ‹¢‚öËò’¸…¿ã0¤8àøú†¾·çÿg¼6oè•€ZàŞ êÿ«.	€F„º¸ìg=¨GHBo˜0;ø2<êTé¡½¡`ï&ÀdÖ½w3r¶UÛè
¼5ƒñÊ5!US€–+Ùä$Ì5S˜=[f%{'lwHwî2GÊôguÿ'@X æ
KG‚€FÀŒ#W.’/0$‰¬ÆæÍñ×5mäæª5x'ğÍ=¬›½Ô+`î`;¨QDv5şXFæ¿óæ¡ÿ½†à©Ál’Ågj„“¬nDìÏ¢G‘”yÆÖã?»Æ´ıŠE’ó"4+IÉDxœaoÌ¡ĞÆøıEğtRSsR#JÁæHl´eÂ•5õA¨N9ØH£*ùœ>2¶Y_AâøM¹8Pğ(b|gI&±D«„YÚTÙEPMÊÌE†W²ÓöÒ[$ÙHè¦ÍF 1Tô!,WòÎQ@„í–M‡‡Zs,ïpVç}¯¿	£y
¼ÚÎ©nc‰úòA¡¦?·WÄBH„sI€‘@µKÙ9–[ JbÿÊ	G8.½ó¡À/ "°dTbÿ äNµ~z\5[ëû©årk/¼ÈnÀk?~"ãJ×ÎW7”ïàëàóç¸mh¶>ıs(±ŞUtå¼›ÕÉ†‚äëâš‰>Rs•${Ï)n…j×Icğ¶×GÆÃ³‰•[«¸\¤ºÇ5bÍé[@A:Ò,m´Uûôe˜Gu…’ùzò¢VÅä›ò\ôè¤óT›1rÅCÜºğ£éêuÙÚÕW&ßeIÉ‰İëoj~Ñ÷Ä–KjıÔŠIcyÏĞ–
³§JÜO/‡•s ŠÇpç§İ×İh×7Ã²²àÊ«igœ¹ÙRÉ¤ŒÓæ¤“ñ±-ò6ÉEe×„ÓÀÔ]9¾vsí(¼´[G+Z® —<J‰Åß´m‘¸[øê¸ë&Rçgów¾İÔ/TñiÔ"^™7»ˆÙ‘ä×Ô`'I„âA×*Şúğ=ÀwZúå-©ËA3>ÈŠ×üv®Å—…SâS(ÜÄÃg^ùY$^¾,aöætmY­’,bÊÆÎGWOùûé¿ªãàºVxÀ¥™ğ[Ãt‡…ëB£D|W¬õ4{Œf³Vvÿ8´Ğ­²7ŒØ% ¾g‡uOËtl9®É¼ÙÑ¨İ¼ĞÀ88]cgÅ¤×Ùå}ê’\%.ğÆ}~ò\•/ğÄh…<İç£ÈÌGëzÎj=³ëßÚ¶DŞxüÑ=4£X±B/	*×æÀ¬¢`*j]*¼×^ÍmÅÓdï&ƒ“Û•ö Ñ¥I{]L‡l<1µš.›®‚d ILpÖ¨”q‚2‰÷>în^ö•¾·ØÜ°Í¿Ã¤ï8€Æ)D3NÙ'wò`L`&Ü. SB¹‹$>‡#Ğguëˆı=BLj©ÿŞĞòş»]Ñ5ÌCA—›f¨(¢ZQ(ƒTTc£_L4f¦£v¯¨s´EMMuŒjbÈÑà‹Ó‚."şÌGå wİ–´]"Óî¾¤Ÿ§‡m¡åPã9ëÑ›ˆi#ş2ì`œXû…Š¢¦`QVŒúP	­åûg,ö»¨‹iÅ¿Åi%b.&ßZNŸ,áº­Šë\W×NÄfQÔ¿ÖN|‘~9®ÕşWÈá½‘gò!ˆ„@–gàÌ_ÚSH!TÖ4êpål¾ğA©·|°g&À¶å#&põ‚\½L”£¼—+!^Ò¾ËD¶§Ñà:şş0T°Ì¾øD×ªÚ"b
kí:D—(ø±dÄKÌFrI&«²¥¥¿‡«n¹u†çŞ¢¶3s%}øsÎ %d€W36éÈ©Hà¬gãR}X>É‹}7n¨Òñ½@‰èœ§¢à°·¥6|DßÏ¯Pgıº:æıÏQÎÛğá.¢Í©=}ÇÛÍëÅx÷F^‰‰ñZ Úşw
ºz©3ßÏœˆåÒï|ó=Ï¼bŞ¡Md;nO}ô'ˆĞş•8ßW!y±>4¨OÓ{äÍ†¾§ò7Ú×Hb?‡LMêUW¨¨´«)ïrÿ¶©tzN/×	Hc™˜É¥ôwÿ†6mdæ¶|gÌÂ‚ûv2
™²Mar?âÎ¨ˆB$}â©bµ•-Íè·¾ßàà‘#|Å+µPñ§-‚¤Ç|êNA–W¼Ë®AR	ï"§6½‚…v æC¥”‰]9Q!Åë “Àïc4ÙÛ7†éÇ¶@2÷g_?~§ÒÙş`JG“Âkpv•oäÍğ¸–^I_« Ï–~oÀÔòÄĞÍ¤[œØ²5æIîºPÅºhâ±O©ÿÚj•´›eÖmÚ.¨áğ¬ûƒ8	•¿ø¶&ÊTÛÆ2µ„+¶»",§ÓdÒTÃÅAO]¹Á·=óÀ·;÷`ƒ`ÿxØÊ}áÆòv-õÃë™wøÖOyè¡PAWYZkß<«8JÄªÑ
ÆâÊhrÁĞ‘„ŞâDşzôÛ§¯£…âÃèOm–CötAØ\í«.õQş¹`ƒ¼æN.Ç“°Í÷vÖÀ–h@¾tOÆJƒ‚ì²/t|ˆEM?ºïä.'!gH ûĞ„Æ­æ¼£e÷¥&}£RçÆG»·¼-n‘¸@’×ïµ¾8™oê¢°0ø‹ùnÎd¤Øû¯õ˜õ?æiwÈkgá1§!W{úÖjÔù0ãÁÜM"îrãb´eÀ]õõé¡.×Ì!Ü“]r¼É¼E^A|JMóå{\ÁE
™$zaÛUùèQÉ©¿I)£—·šìˆHãîDu4ÏmÂãõG2¼•¿_9¯â¡ò>§ğ+Ì:û[¼y%V/:=ö¦Y“æwıø78Yƒ£+WVÃª	¨f‡-)ås7$¹ä,=Æ&Ñ˜¢ÜÔ†]q~‘Ì¬èÕ/·<­üıèŸ_dWŸïCâS	BŸÂWÎ£#¦<ë§îPhdêq=k/ÿ(dæîS©oR™ İ•ScÂŸJc¾Ö×¦¸êu–,#1ÖĞò¸œpat'Íq®˜2/fí›icõ×K˜-*üh;,ëW¨İŞWóˆæO‹Úèuƒ½/¦O­š%‚¡ë¡,!—ÿY,š°z€·E+aL:`§Ï±Ÿ´¡×İÊ;z¢œ˜ÊtVÆàFV¶±ØıYËÚIÉÅ°FFK.©~ôfÜø©©BŞÖ¼º	ÛM¸m¢Å )ÍÜM”‘GA¶‹£
&ØY!2iK“Yád<Ç<…êf?åÛûÓ&.‹C‘`ÉMÛ¸ÆŞ' ­|\†E[+´ãøHÜY¹*ÊÚZæ‚<Õh4LTÒbĞ¦û%ğğ’OrJ•H]¦KÍ.ÃÄ ¢É¹©íÏÄpytKY¨Vl(»6~„â°×’|Y0*ÁëX–ÜQo‰J¼UX}óø|¼.J[¶`6•hGVŒÜo2_Ò:$·‡m‚Ä¿„'¯³MŒøÉ°ÃLŞú±Şé"„×ÎÆ‹tşHK™v›;\–ÑqÌ?eûJéì4]HiŠ5³IDdÃÙ{áyş”³{zg CT bÙİŸ,mÈñÅi·áÛDõ¤áÖÅz\ä<<yÍÍW6ÑáÍ^;p7–Mt”dÉæ¸ÎcNÕ*ãŠ¿½vÂ& ÏF¤+á¤¦„1=:öê©^SHÆVÊ^X0lˆç.¹‹—–WAæ%¤~¡ª6ª“^˜qaïıÁÕaS Âç¹å=Kæ“’»YçŸgËIyE‘º®S#kYŒü¢#üáè@$ûim’ä¼Pôw.œlV*Ä2¨>w[µ\Â¯¬IG|oôÃ?Ì2:q®ßäzÌ¾ZÎË‰l	D÷$¯p^¬&[®9€ŞldŒ`ØæU¡ÌbÿÄıº”ĞËbå«’ä«ª«’ö>W¾:B:”bs€jÑ¾ØD²Ä¶]¥Á=Â1¾¨‘n’t©"ŒÏ±Ğ3d€>óÈÊ6ù³¤~ŠÙ‹9	1POúçgR«µ…Ê1<şMÏwÂËrnô-\ğãjl—pwò®yd'ŞYË„Ö»KP/ì²	U=Dí£ˆ@ÅS«˜Ú¢ WDÈPU«÷-'p—j‘ñıäpåÿ3@Ì¿K±ædöI¶ÌŞ½VÎ ›’+—Í_¼Ø=†zÇNÅßó?Şa²~NÜÓ-È6üm×	¢É|,1@µ¿>ç(9Í6ßÍ¾ÖğãoóUë´õòÍÔÊ‡Qìú!;Ü¤&=gü!;¼¹&ƒÖ”î´‰câ—Ôõ£s’¸ ¾	bˆP[Ò$Z¨}nàĞĞš+‰!Œ¿‘#ZHó·PDë:Ûiú¶Oû7sÕK5‚†é0²'V?p¢<[?Ó+İ
²ÃøùoOfc#'Œl7NÛãv³£4¨Íú@ƒ/ç9.vÃ©$š„cìƒÇòX-\-U>[©Í§!³•„˜lÅ{F4¶bUm[Yâ0—6ÖF{¸\(}Më“ŞŒîÊ_ÕĞí¥³Ï›õj¹ÆÒÓBLí5M<–Ù+-½IÕÙ£Ø“Ø¢(P´CáwªéÚ_AÒ0mÜ%&sÒ²ìZ1;3ú¦[DŞ§g/i®Ã¨zBŸ
®’iôéG®N5?>‘5gWhNK¦ãÅş\“…©üËæğó&ZÆK\95¯¦ ¡¢ã{Eï˜©¨JZp	Ø¨+2)0Å£Ëî¶ä_S Ÿşp·ç¯aö 2#àO¬àßcÓ8mÎêlj
ûñ^ª‰ûŠ«
=XópŞãFŸ}“?“¼èÅœà–Š}TFgÉ@éæ}ßP^ì±òá:LÂô×Ã,Á&è¡çP˜„ö/ÑóşÃ‡×6üßÀF(;#"¨Îù-µç$şoI¦èo™xÁÅšºpYŞ0}a:ˆ—ëÓÖwó‚»ø¿§õ|íû¼±ãMæh<6ô<÷|NA¾;B3z¸¥üİÓº¸’Î¬®`Ö.>ÆC_¼*bµ¸¦µóóu}ôA¹Ù=8Ã›ÿİY‰‡jÊ¹ÂzßªÍoÉ?Ò4øãş“é¯O|Fë5^Õİï¶+>o|ÿD.lóã¬^„Fç&¤*L?#²ŠÛ-_È÷‚r4?Ğš	¤IÍŸv›G–‹m';Š°¦“6²[nQöæög®¡@šf'â¹N;Éc/ƒ 6}Õ©¡´“é>ö"¬‰º·ãÛz±8>6¢ÉvÑÈŸ˜ìú·³&»S4Ÿ†©ÙÊµĞ8¡áØo÷¾‡wÎ§±;»c;/@xÜJÁ;6B›˜Š{şúra(±3q«~ñhè¾¶j8­t_{v‡©g•q@UÅâ>‡Ô93¡_fóÏíHÓÎÓİ¡P}nÚ=-rıÆºiE!¹«Ô³B¶ t\ş‰D”ã‰ª*Qp&Ås²%Úì&`ÎmÏ*bxhÕÀÆš”ı†tŞïùktEêËX±mô?p,è¾9óÎ_ı^ ¤,-\3Ği¸öcéÒ‰á„Kl+YK§Üã´WcA+ıÀa€›òÛÍ'ösÛŞI\wé¤oìªYQKÉ5¨H‡Ì‚SÖè]'[?ançóÓnc>­·VƒJÖé¸r™«GPatğØÿ:U¯ô]pĞæ¤›
sğı¦TØ04º!ı5JK¼„Ïxµ]¼%ÑMo/6ëxùÉ€É&«İgÖ_&ÏZş?ˆƒy<Ñ=D;iÊ¶Ğ”Î|64šûÑÅ­[`7&éí^jõ]š©¥L	Z„«l´‡¶K:ÅrOp­““´¥Ô6Ò®ˆ¼ş?Ş¿ù…¡¸Äô”ëOBwÿ,¶¦ƒòÖ¥LnŞök9Õ¼ZVÂk+õ„­sĞ£ğMÒ6¶8~à!Gv(“úL®1Ëí×™òæ#–h±Y¯ª‡ºò"‡p»©/Fkµôıûäå*hÃÕ_1\n(ŠÈd*?ë†šuUdz®Ú‡»úCb²‘9ÉÆ‰Lš¸e‚ÿh²Y‚×Ñİ&‘Ëæ	óóÚ‰&+{•½èL¯5Ï‡Ì´\Q~\4¢µ¢¤[n5³ EñI¹¸‘ïŸÛ‰ÑY+Ô~ŒÄ¨NôVœ’˜ É¹côÎ0u‡`î=4¨áÜêhPo|Ÿk¥A5÷*§A…É}¦£Ë§ŞA8:_qÌ5·ñqT[;*¤ğÌ\S5­ÑÂôßºE>9áıã;+{ÑÔ›z®ì«âlQ<î$&Ì—µ¬-'YÖ2iâ²Æ4YÖxİe-—1ÌeŒ’ö\|î‰Nb—¤ù¦.fŸµc¸İkŠñYñzì:/ÉP1Wo‡ÖÑæqAÿOî'&p£Üõ\/öø·Åºú›ä šãAuûeémù.ªZ‡SÚC§ñ>¥¢®¨¬„‘_*{€¶“*—>LÓõÍÔ»ITÑÈçâjR)W\“ZL…I•²v_#•8^H2€Í®µÆ’ùPÉGÃ
ñ×İæ¸}V+¢şä×å­¥çí™Ã
G¢%Ê»+Kt ÚI«xq\Ó§TÊı  ÿÿ Ã,ö·xœì][sÛ8–~ß_¡İ‡-w•*-‰¢.™éì$NÒ­tÇc»{¦¶T JìP¤BRN<[óß /âå€$(È"ù!qB¸œï\qp`xC;øs„«?=Û%ø/ÿ6ˆv{Ã±Í·y ~`{U-¶¶+~¶	^«Go/îÂsö[ÂZ!\õ±€8–ø#6&h27„øƒı£ôÄvÃF! 3´ÈJø<ğö¾	?ŠæTztú¶» ÃÛ‡¥‡GùÈÈjƒ\¼òQH—šİ\­}o¿»FsƒüĞ6írÃ?ØDË ÷ÊÎ'qCÚ™W^{>¦²ñ*‘¬B/DÜÊAAx·#èó½Mg>øñG“ÂËÛ–[‡èÚnwîqUË„¶üáÙ¦LÓ·ÄA	â7Ò1óö¿ï•kF¾òšã-ÃEÚZü};°§¦5Ãk|ÿ¸£4oøå[†•_(TªÛÇ ãĞxçâå‘°¶uõ/qÈ³ïp1ùö—ªf”B7™o¿÷]sá’Ço\ÓF>*¶-6Ø'ÍÁÏ Ô)`Ú©o#Çş'¹úàîöá]òO¶~ˆ¾í´ò.MÏ¥ÿÜ›¡çSl‘o&Ù±1ÿ0ø¿´kövHç¸wÂÁOwïd8†ı_íĞÜ®2½?É~Lë¾È¼û²ô&û9ôO¾Š¾rõÃ_ÀwºŸËª´büæOÆ35#K?'1Äåşå“pï»ºC”zaR^
†ñ°†)r8ĞşĞgÔß¿"æ!È?)šıXH\À¸ù?ª-ÛENœtGß&HÃæ™e†xğlLç…0í mƒJØ×¢ÜrĞ: F/¼`Ÿıà†Úä
ZöÃ•?}òï{úîÿÀpğşãëŸW£B[‡X¡ é¸ĞÔDîŠ™«½Ëz¼5)¼•±uoLo¤– ½^œ®í
Z. …‰­"Á+Ëâ`ò6’h©ŠËÊÖIÔ¶¸BYJôNqÎÌ¦¢m™}õ"Ïi	¬*ĞsûaºXi¶5¸‚Æ«ı ÉÕŒY'ÓI^¼D¶ŸŠAÎÁAFo?>Qoã1¼&Ì"Í÷Çv“ då–&:›Ùó^VnòşTi«DrÃUš‚ƒÍÚ`İ3¼ÀE_¡aÿ«²t‚÷ŞÿèÕ§}(RiÅÑÅÃøêÛ!‰#kåG’ê:–U×0à¯Œ‡\ÁI¼1õœÄËÓaVİI¼¨ZOâµÅ)?‰–Ãœ”YIºú](ó6]V¶¤2¯°5ÉhÆ¢‚-Š¿¨ÉeğÆ8­û"STG)¼ò'3*°ZË•ŞŒôŞQª­üÑˆ¢*õY¶X‘q§R‰æA[I­©Ö\MA¤Lí(îZy!‹Z¤BQDÊú`©™:c"gMŞ#ši3‹\ü±‹?vñÇ.şXeoìâ5óÅ-º¸E·èâÕwqq‹:æ5qT,Û!¿ `“ñI>³biKc¤éÀ¦ß>ö,‹²5¸İêØ[»üÄxÉ?ş—Y¡áÆkv
§Üofí-İRÌügv#rğï?åVè¿ø®ìàe²™¼SÜm7s}´Ş;<©o‘WÆˆãT—1oòíßPˆ¼ö}ô¾snÛ¥,–¢EªÕ3|e*Z¦Í–DrxK‚ ­I°bºğ-±GU®ö×´ùØÔôi™WùP?ÚAø×¨»WwmÈ¿ÊrCéí_£‘¿$S|ÿzƒÂW“ş)ûæï3¯{úg³7é2ïyzOÀBJÔÉäVI1*O"ÑÀ³Òj #é ÷›J=¸ïnJÀ¨ÿ l/ w,šÃË—¹™Š±Ò+>3Fã¯&,PñIÆ+¿—0FÅ÷5ş^Ä3?•ØH•‹{nÅ¯Sy&1„š6OĞÑ´=§{ÓÆœ¨ašX¢MQèíx–¬İC°‰Í‘n”%¬8{ôo2HR^Å¢9ç“Hã¤³¬ ÿOFæÆ¯4³iİ”¬'Š1CqÇ·4'ÛÆ.â)•üà
Eı ¢¬1kª…+ÌEÈ¯3½½[öë¾sP LJëxJ‘RêLQzğèÛX7ĞÌœšåÚ¦ãÀ°é—üTe ¾ÍÇd´œZZ»>ï(H»­Ñˆ¾œµëø7/üÕÃ¶e¬6­¸Hİ“¦g•ò	còª;Ó‰·®/Æ¹
ãüÜ*°—†sãK%›%òóÄlÆ5¾LLïÂ—Ï/¡¶ß+÷f´¾JNL‘.ñp÷‘×„€;ô¸%nxK¾ìI:_ËS—X–¦f-˜8kÍw KnF´I:p‘ø0ïnã¹Â‡d‹ìòyY6I/_cL-Ä`lì;ö¹BÑTÅ?ÀÅy’`Øs625	“@ï7™À}w3€"¸)Ú¨ûÔ«§Èƒ7ÅøOĞÛìsÓ	z$2Ø2FùqŸg’W¹-”»#‡Éì7cŠ1˜*"ĞÇ¦Bô]p@rJóL
qÄ[ÂÒ<·IüçŒ„h½b¿¬l^WaŸñèªôó§TNi'ÊÂi‹¥¦[æ¼ahËû¿æKñnëıi·Œ¨--OÇK¹nßmwác»ÇÆd²˜Ù[Î0ódcù"Ù·Õï)4RÜ~Ù#'¸úÛo”ˆío¥xõÂ)ÁÉ
ş`ğSäJÓŒg`ÑÏ’²@7v@¹ˆr kÏ*AeğŸÿõ\ÕHØ)]„BŸ?şXÓ+£V]§¬Í…ÏVöZ;SŠÑú™ÒFÂ™ŞS‘ø{h;Á‹˜‚WW¥wl?¼ [J;Ï 6™ÓF€­˜HF¨ÔM'ÓT8©Üø²/Ä3Á¹g64ïâæñ¤2íA½€Ñ&4óD;¥¬#ïø%2]™›ÈiãŞÙ%n–›×1k23	IÇ>/‹6Ñš²=É"ÉLœ‹CóÖ'FSdXtsŞÊ89±ZÊOã-‘¤=1#'İ4öE»ÄÆ©ïRRJRáÕ5	9]y#%I«óéba(q(.¤§İeLº[ìè§U;±^K 3{A$ m¤éŒ¾ş19Ì	"îèãÏÄWŒ1Æ‹éÈÒ/Àè-0^? ›û@ï,‹RE	,0A“Ò õßIX—à‚¬Ğ¸ñ=vªæfã…:É1#}º€vÚ;	‘‹äªî…¸ &Ög&àğ]pÑ\ùæŠ
s<6GxŞÀ»(<çq„® †¯K}P;+ÀWgÓ£@zë5Õ@„ø÷>r‡õU"nÈtnú\`©À©	¹±0£¡\Õœƒ's¤âüÈ{CÛõt5µ-*œ´ĞA´¢}¸¡âÏÅ×&*°‰fóùd:µ„Rï£®ÜıÖĞG;;Î¿ÎF¢±„Ï•IçpGBv"`+Ïé’3²…î+ªQš%g-š#ÒÖ4!r±eLRµà·×îï;Ğ_Œ!Ó‰XáWB?×†!†yÜÌ²ı \U%’±’şqƒó3 _é×ôÏ·ÿÕ¼øîX ×:%pıR×·M‰Ş–>¸Ÿ¬‚êõÅø)˜áĞì‚õ~cığB{ +åXŸ ¥ ·6¹÷D¸.´xÇRÅş ·l3ª Ç“ÇV™ÿº0ÃÓ'2˜eÎ“,ZŞ&ıÀÙ’eè¦¢“ã­)…•Tµ!ó‰ ışóytvk‚éûÈ&iÈjŸ…½µír9¦d·—,g£ñÍ#—Ï¾^wß­ßX+å0—ÓDJbqK#<F]ÀJ_fiH¾í<?OD%ñTİ°,K´}ƒM0•ï|:"Z‚Ÿ©YÅ×»:ìíIĞô9šc@€ïÓ ¡Ê¸º%û«áÇgk„Ï¦W•áÄC1EN‹ö"‡Û*÷Şg¢QÆœŒ:t¥ÃñïC™š–¯bòÑ/5¬cs&XVö™`òø0wñü:£gº²¶‘jÑ¼Ô‘©›8ËÁõtÜI wp|•Û`™lŠj1AË)ÂS@gvñ*<+Í¨ æ†˜Ÿo‰é=ÿñ†>ùêùJö°63,(u!Öo]‰â÷Õ—*¶UMÛº•@xoûÄ@¹Û*qšD[NfcôTáõ Y$|\¹$\ÅâHğ9=®èÀÈš.ècu[ÛV»}À¶úMŸ•Ä¿ ùt/í´U ¸¶4BôGÅøy&–¨Ë
d©Éë_ò¬íªÚõœï¢ÊŸü,e'İ¥Ø-¸ÿø¶1'ƒeÆ:ÄŞÂJ]Ò 6ï÷Ã&­ÄOŸ,}9 “fĞ™Ğ`´ìO¶}ø6V•ÜEû‘rPDùşD‰81§HÓ5Ò	#µ•H8¬M´,İ²$¾WAbãÙ’Œ'‚ˆoW¶Óé'sï+Ó—£º-6ùÒÕˆb\*ñ01£‘A€£#ÀæôÌ”Œ;•’¨«6Eyİî;šÄµBã‰µ&	‰J`-ghDF@}ì™¦¿G7¹<;ÄH›¡¬¼yäâ@Í©õÑX³ğ°;pˆü±WQ ¤³B§Ï‰"T?´ğmAc8ùYÍyÔ	YÌ5©Ú´	õ¶«€~Ç&ÁÊò½m©İáœX7\Ÿ>£©îä<)ZÅÆNàPí]e@5t}„µ	àa_€zªÏÿC*Q²sµD‹ÙÂZ ÖÚñXe_¨¿D÷üN—6^×mJD­ZŞÛÆùEj6k§/gP¢RÖA	Éš¹'Q¦ÔÊd}wÔòd~T_![´<iûLRPºômÃÓ£ïĞƒY¹˜/umnç§.z÷$Ğ¨ØÄ}t×£š=z}<50ÛÓ…wãJ+ËÜ¡ó¸Š×µ-NBxjvO4!ĞÕßõVÉ*ÚZßùö2WéúÃÖzº£Vxx×âĞ;Ü ¾^…]“J×êìpóÎí*şëA·z_bk’è®±´* rB/¢ø½;5'íM¡›#är.Os¤ò 5x¨BÁa-´1šŒ€B0]Û<ï9‡Àqæñ÷Ôó¹%;ÇV¿Pÿy9›˜­ŠVñhzRZ«®ÙÆfÎÛ£°™O¸µììˆoƒuGN|õ™uE°rT•xw<,ZâİÉ0KE}„˜#\/Ïy |«H‰å¬åT›&[Ş:?¾UÀ7Áwğ”¿l½Ò_“OU„s&“‰…æ 0€pNww›ÓåIÖ¦¯p©
éH‹“,h’{HUœ~YSÓ€êùÕ«mºæÎSÓŞv] â$ÿ€ç`âCç!ñS‚UÏ·›?q›¨¨%ğ8¶^xÒ©D¯Ò…ğ=Å{=‡K;÷¯u*{üÍ•	ó¢Ğ>jÃm€r ¶ñaáÍ Õ‘em©xù%²º”¨¤éD#3H­+Ö£-<®fÿ&Âƒ…ê÷Ü€'[ôMÔçÖv¡Gİ”&}WŸÇqŠˆcî¨T°*s-8Øê›qà”ñ£"é¦ÿ•„ÿµ„§W­¢ïŞ-¦ó×2fF„ˆ/ï±Íİjë!`;b\&Cí¦ñm|Õ«ø?¹•
´¡)†Şn•
“u}o;!4ÿK(~„ò!©…%ÕQY$3ï/Bó,æv¹:¿2T*°’c–ŸĞ†Eî¿ÑÌl«bÇıËQ§@ãu”ÚWu%8š]uµÂ×JÓ¥óÄ;Ñ™ÓˆàqYÙM$+Û	‰@m¤¦²ş¿SbCœóî‰¿>YT¶=Ø&‰‚ÜjÎé }láNdğ¹Bí£âhLÜh1?m%õdÈ|bÍ—`•EÆwè¿ï>ıÖ‘íÄ>ç¢¨ÛKLxÕ‰ÏãYöZÉÜÙlŒğH3Ş/›Ï??ù¾‚¢ÜöÙ·ß±ùm•¬ÍFT6 »ÊgÙÍâ³Sì#É¥¸Û'[{¿½¡Ö®§ä<Èbl,5ìPœ»ÌŸ;=YVÎ×RBKmFä>äÿ°jIµà+L¿df?ñz*ÕáÛJa1›Ë±1Ñ ¸(´ˆCz¢< C”-ÒğdÎÇW’$uvtGû{_ñÓqÈÎ†ª¶Ö]T ó¹«0JGËù|:éë™Î®pjLÆ‰R:ÉD›Ëû‘ç§sŸM•wXr$`;ØÚAp·_¯I ªô»¥°ÀÕÍö‚t4¼œg¯7Ö,B´T×ŞŞÙPf6”÷@y³\›6› sMŸ/æˆlÜ%Ç[Ü5|˜8ôQÚn›•;ï+ P¥Ô8E¼$İp¤ßÂ
nˆ6ša¤6È·IvsÏ/Ê²„,‹öMpßwfKg)mîH,J§W3c´°–“	€³ú´Ñ?÷A¸2é¯ep<xŸËRPÃĞ£fÙİDL»¾‚¿&-á 	¹c"N4} b¯µ}içÊİècöòá‘WvÒB°ğº*³F±©³tZf‹!Ÿ¸‚Œğ‹y¦ä*Cõ4­Hi9œ”é¶]nÎE!‡Ìr]â$õ1nüÄÆùÅÆXÑU7È0ŒåØäœ{`DhÑt¨˜FÏOÆ+İ†g)¿‘ mMÂŸ}o¿Ş{ş[;0÷A ,…Öæ@xàŒ‡(¾ûY*XŠ¨;S—Ğ’“W‰3ÒÃ êŒçD‡á{›((§Xçš­Ù°º€>ÇÓµn*>øª++Ààmº^Ó0¤¨8niEµ8êtTıÆlüb~Nî
»»½¡¶Oô{@ù})5É½Ø„P'ÁñGÏTv¯¾ 3ÍšˆjäŸ‰wã±¯‰·Ú±ßDÛ7cŸê¹.À·Ï2U»)YØï‰)×6ÊÅ/5l^cêê(9E¿Ğ<6E7ŠĞŸb¯¼	~lV÷ö‚UÕW‘Ä´Q—ìÂŒ„^mºµ×>Õg¤Jğ9YÌum\Ë™ÇçùÁÕs=Ş_-Nåşìx%íiÎæÎÆ“šj­*w¥öt°ò\G\“‹›Ñ5m¢ÎÀ&ve)‘¼màm·{×Ë¹~Â¸>ô¼y1“êc¼‚2â—sµ'?‡E>;W›a¹Ó´‘xo:<ğÈQpÛß›úA¢‹¨—)ØK:³vÁxÀÔ,îì‘S¿ÜVIÉ™æ'ZÛT¶/ì$åĞbUW•ÒFëêk.u˜ìõZOpD•µx¸’—›j…u´JSgkï„t*â6/²LÅJöŸÿÅ…Ìà%_	ñ'®
K73³3¯Õ0ìpp„n©ÛjÏ'Lƒbih7ÈGÛ r*õPJ4O$Øg#á ªûñ}V%•ÃØ)>0™ı‡Ü]	ı2FGe±œ¦Ä çıãË%1Ì¼Û6âV_,§!Râ¶A²FEY;:ğ˜Y^GÕF¢ºå°9=!W?£ø‰‚Üªc­È†•B"Ú«
€g8%™’-`bMÖ¬•Qâz«¯ÄØUĞl‡ áğÔ™Gægæ‹¸â8$ÏÑ[aYâÏ¸åù_‘Åcİó(Ï* «ñ™º0„¹M>x<ºÄéºc	!Çñ¾®vÈÆ+Ëñ<|¶w²B…?feÑï=VIÚy\…(” :¨Èƒj>r1\~ŒşWäŞïâ.¶ü¥ÔG :¹!xïq€"mAGDs|Û«,FÁ¾B©ı÷=¥,ŸçİÆóCs¾°ÿZEóâÿ„WpS~–¡1å&¿Ü}|Bˆà/“:¬€’l°³[^$w;ßÚæ&afŸş~(Hs·![â#ç–˜Ä¦ø|ã…ğ_?ş80÷Aèm/áTHİ0/sFÔ§+ ‹„/9ê+Ÿp µ¨Ø1<ü8 \70}lT
Ñ…6öë±·”>ËsJœ;Aö»í.|,ú"ÉÈß°OÅõ>Ş¾{óûÏ«›Û¼¾·úãİíİ‡O¿Aãg?áÆ÷¾ò¾>8Y#ç*ò.Yô«ÿ8LjïÆ%&ş`yÅ–!ûQÓ:(‰÷ôa¬e$Ş™3ªGâ½ù0«d&7f´”Ì‹tz"İ%ó™Ù0§Ñd^]A='qd¥ürr±M9@m˜“‘OVÊOÎ^L4¸²p |†½¼í“jÿ£rs³ÖÁYKÿgñ›÷5njËç¬”V]Mà°pEW9s§İô)Ï‘}¤–lã9Ød[)îxÑda)ğ#ã­ñU‡¤}Àv½	Òás’QqO)Y9zškÀ
ş*U9J* i#2Ÿ·º:â™z¢JœKŞ*"ÜËÅûì©÷yq#/nä÷åF^ÜÁ&Ÿiï6öåúíˆñeQ^yìâµUõuñÚ.^[“>…×&åDÅšBœ«›KmöÉÇÚ|4}=¿>SõÕ©'JÑhz~Å=¼¾·[¡=ªØMâm¸Ø^™ˆ›&U;—µnZ£­½¤1¿œ#µƒ*œŞôÔ$R¹„o~¤Öı«ƒ#ÖèíÃØBOœ\‘õĞÌÅ|.Nåƒ‰·
mÊ!Ú–Qq
Ç-fğ'Lé ÖGa1ÌË8+]K}|2oN†<”ù»:)b®{5íıêRA<Úfjö­‰ÖrÇêøm/}XP/­ü¬Tñ4µTçùA™"J[‹\œXßëı-Õ¦mIšÇy™O(×çÅ’ìcÒ'“¥n— #OévQxPdÜíĞVIEwk¬/'Ø¸T?­å§²xö’jF ;†¶»TàXx‰fHT¬§SxÈ.Å.³Œ¤sË…Å§ªÊ$óåH#NĞÇ»±!í¤‘xÉ?şwàíàƒ¥S_¹Q?å‰¯\ÇÀi¯ìó'½²¯KòÊ÷|ü	¯¿}z ¾O0yUÇ ò§ âÑ1¨"ùWŞPôğpøVã9åpH<]¶­3ôÊÓËLâõ·Vsxî­SrX®£”£w–ã«š›AnY¡ƒÍ( Œ¡üÌg¯72S¾,½Í~cˆXSô%hËšıt%>—A{ñµ·İ7<nX‡ï(TD‚Rò‰ı«^ÜeÁr„‹ş¬Ò"¢§”ƒ­¼š´FdJFÆL¬&íĞ©:9şW±|¬»áë‰„;›Cé€‡WS3úË—9¨©ßÎªØ²Õ„ªÒÅğJ1RYx„‡*pÏ¬Ñ™p]PıŞ\4`7Jd">”ÓXçÊÕÇµ“y4<°ıÌšªxğh˜®H«@°*cH ¥T!M3§ÆSà³[ h¡*çdW¿MR€eÎÈZ6ˆU·ßÆ.ˆ¥ÂÓ»BrñynO·Iš3£y,„ıHí
55Å¦w¢:Ù€¯$®¤¬ö’,õÉ8wPÌ½hZ}çR_½V+7Û—¡`yOıdeÅ/‰1péx×Š_f‹­B_±QŞˆ:²fR´÷{_*BºÚ|¡Y@ùÊ¢½yGöü éyfPœÛ	ÊVpºÙx¡’‹Ï5c<›×Š™ÂÃ¤xi<ûó«'ƒ_oåû“cFR^Ú[EY Ét®kDS^^œÇ-¾â• rìy5 í>Pâa6gÃ#êÇÁ…„Šm!ÕRT‰Z4/t^kÓ
›«Mt›W©™,ìÏì‹ÓšX\GGÂqŠşMú²º‚ûÚ-ÆÀM^bägC51~_EÅk*·Wø™ĞIøÏÏÏ\Î7¬Ö™ÎÓ¹İÊÑõS\¨ÉÒ£Îû¿³ 
Ã×È¡Ì„”h”Bš53 ·."š9¼Â.±åéTRi~G@uşøi/Š ²=o†”	¶Ê~—¼ƒ5‹½ãkUÉnÏ€RR}F§ß#®g…”dbb<], »åÏÂÉÙ¸IÙA’omË">qM%ÄœèKm9Ó¥öÌÙ“](üÿUè…ÈI|V ğ@ã—°—½%‹}Xé>Â
nÛÀ´§­(›1\HŠÊF÷l|	eÓQ2\_O¤–¹´ñ\ÃĞ…Áõ¶åù¦à&¢º[ãçÍ-"/w&ê”Ğ©D¤¾òe»Î‰ßŠ•%ïz<ö²œ²œ(·iõâQß æÏß£ã$Ê6–d1iÀI\‰‡k®ˆiàèhüİˆv–8Š¯?S6Š©ÕŠ“„A…G©Şò<¤²HYÊ3‡°jÑ¤úSóÕ]„	)&äùaÍÒsìÑ¿ã‡ü\e¸´Üìñ]ú¸!ÙzÚi½ÅŸ>Ÿj'âS]1ŸJÙ¿²ŠÏGƒıpÔ*®[kĞ¿å¬u•g­V}Á‡éË¬xšƒË1gEk\R*I[ÌMknM*ÓßA—“àâpøıÇ·‡s)d<Â€¾tšS¤Vyê›­&½É º¦éf®×©;Ÿ˜v ® fbw•ëÎ‰xş¿ÿoäFáÇ®»ˆœ>lVÏQKRk6¦¹ÄKãa­ôk–NâlG¯Oâ'nˆ³ãÑ^©WIÙ‰©µ/f@RÄY¬?3*AÜCDËï7‚|„oM[†¦&ÀÇ³PĞMæöÜ‰øz·‹’hTQŸL°æ@°ä.J¤¼=ŸÆ|şéäûHåbÛh‰¯¢%nuBy6&šµĞÔ]Öšz›.áº·2nËÖ·
Ø¦9]X£‰5Ÿ›)¹Ãé¼¼?íõİqÃWÂşª;•~6Äô¹n”¸¨9_ò–bãÏ5»gÕYTœDÃÚr¢M¼ì³˜|’v:Ã>R^"®B©É‚»ê+Ë±‰µ	$@³DzË]Ü-ú&z­3.n¼®Ñ‚ö<ìçèDú&Ş$u:#‚ÄéÔ™Wsc×r¦°7Ùà®u ¿Å«¦ÎâçÙË$$–¿[HÁ'²ù½o7êLïÉÌ´K}›ŞÄ °!{ÏÄ!î:ì„}.Q²>}a¹mL‡Úv9š´¯¢ñeO‚ğkúÊê7Z³)Fdª5SîâûôâJëU'VãU#üüa_x´9Ï¶<ÔÀ ¤k}ĞÓd«§ª6ÌÑt$C$şgÈ°ÂNQiÀJû3y\Ñ!¬‰¿óm·öç³€n[E£Q«ÅÔk}æÓLj`*–´ÚRC	4zõ¾c´W.<öÉ»ëN¹>‡Wj6ó„P”õ}d:ÔTDÜ?îX”^IaçñTPÍ¥mŒ˜ë‚Í3ÉP:ç«ˆímO„Szÿ¡_	Ê¬©1[@±1ÊØ‡YÜ¡+‡¿GŒEßM¨Ğfü¾bü÷=Ù«ÙÏÔ‘n3`S:MGÎ>õ?çFƒüá BÕÈ”ÿU]p25FæÂy‚U"çüù¤ŠÌ–Õ]”^¥d‡ÃÄÖhavÂxNwq£éõ‘âÒ–EÉ¿öÜo«ÌĞ\³æóêlàšÚ¯@Ã!¦Âoà"éıº}„ûi\µ	h7!2Ã¤JC¹Æp¶ÅnÅlShX·ô†TF½ënøÂ&
€S0ixxå[}‘Û-Ìõ	k—úî+<k" zÊe3"/ˆ@qõå$û«)ù|xÎa±mÕÔèÈ2S†ƒbØÇÃ=÷	;”Ü	XDÌöûÊ
­šĞLY1ƒø»1­:~”RˆÚ÷ŞÑ¸ª>m•4i£6–sm:_@·¾ÔëÓó}VÌL¢Jİ±Úm«ŸÛ®c»âS+»ç’•‰§².Ë¹XñŠ\µ­XDSÜŠİN½«˜yt–¾fÎh'¾è7X³ÍR6PƒÖŸ­)<á»CpÊk	+O¨vK}÷UÖÔ¨İ<;È«^Ê#/M†YÆ‘;U›á&‰§Ã<‹I¼ªó|'³8te#n”y‰v˜ğ¨Ìk³aÊ¹2¯Í‡~V´ëÒòxĞj6m“«µRMt‹BEû7cD´¹† ‹+îêQï—…ñÍø—’ÈëŞ¯}%ËÜ4º®2f†öá†
b~¾¡ÿóÕó±
ˆáñÂ˜â1`ŞÄõ’2ıİİŞvñï]@_‘×ôÏ·ÿ´ì)®’UUŠ•8×,¡Ş-1½¢fË/–sÃ„¶dÎâ°ñéî
óì#$hË'©RhóÑrfÎ¥r[b÷İôpÙ3aC5Moï†/ÒÄô/6wÉW–÷ÀÿÑ =qÒÚçg„<ÊßÏÒô4•Gbøî×t´JÎ­#2ë ¶ù‘£àî·ÓkÃ–vùp3º2t‘úù$*]ã­ñwÇYª6mÒW}¼Œáâx1‘kGÏŒ­Ñt¤Cõ9áŒóc¿ÏŞÉ	‘,wıÔ7¶ëÍ/¢#©J.\™ê:&ËV™·Y#óq_ä±#ş¹Ä-‡´pÃyå‚E%€ª“Ñ°}²kŞ³…Ty¾«ºí‚.3«Æ"„Ñ/w{#0}›CñÆ·M^ß!ó«]ôŸ]à­wß¢Z:dô•ÓjâÀÉÅ ó(l¥Úd-R¼PWUõé+÷“aVıÀuó a]HÒ¬¸|"Ä•§1‘3÷ló³Z9¬„±ÔxŠåVWyÿ…€Éc´²½jåx¿ôğì¿=Ûm¥÷ÌÔG#}„­+1uÿ„»pP÷è¤÷Ñ'Ÿ‰G”$kš4"&tŸhDÚCgAô«hŸ·+Ò&w–'}á“,zûcG‰*:0ä8 æbY•¶=4ú½Õ^n™ó…E¦3ÀÇ@e‚–ÉÈA”?V7ô'”yuCù>ÌÏZ%ôk‹á½[…âö'P—3Â<a9Œ_@ö;®"ƒ&¹sŸ¬øO	4È[ìŞ÷ª2â6„ñŒbœÂãØ¤‹j™ï¶]€[ÏKåH”Ú?â°Ÿ dN±el@f(|„™|øÃ&_•Ñç‹)Öp	[®%ÎŞL@h»¦O¶¤&²ëÿÂ×¸¯, ƒëÆ§QŠƒàš>%ã1î»äògÛRiÓ-gP$¥"õŒ5¡ÒÉ÷ke¹xŒ÷Âj·ÒÕPm›Ì‘62h§è[ÏÜ3àBåë*î]6.à¥Ï&`»‚M\¼D8B¶|à	ÌÔHŒ¯/®p†ZOÉr¹”¿‚º­iÉÿL¼u°&ŞjçÙÀÆ\lc~Ù Ã5~˜HÿíÓñ}¡ÿYÒ3¸Ü‡ŞûÊå¶Ğ&Ô‰lY¹ıª[Ç_@›ÃŒõVs)C]²,‹ì¯$ü:2­ùlÚêLS`;U{çì.2vÁk$“şé¯°,ñg6”oW6j%QnÉÎy¼÷(ÇÑ¿Waù‚»—–?æô­pxµsCğŞìıFÏ(»Byú÷½m~æ³¸Ûx~hîC:ú_«hFAüŸğÀ‘ãx_©Ÿfã(C ª¿çîxÍÉˆ%$Şa§hR>‘xo>Ì2Ìœñ0e©e$Lz¼`nÑˆZ&^/ÄKm„IM³%rb¢]¾„wÉÅ(ÎÁ¾ ™¤øBÅFY!”–E¡§&P¿+=¼\fôpµ¾n“ca²˜u¢æîEr_$÷Er_$7ÔqËØÿ;l‡œ½Ul‡b4[`Mt|±şÒ‡óKX pŸ¬P_¥®Â›äÏéº•oI‹HİŞ\¿ˆõY›ğ˜1ÍÈl&Uÿ&ë­¾cGV±›ô@üNË»½6]Õx}àşÊúÀ½rôW~t,í“ëİûe	»¡>¾ö·M·ˆ³¨öŸs-èˆ
YzµíÁÎ8$æ —|x‘”¼µÍM‚"Ÿş¾J×¯ü^ROZ§+Óo_…DMÁÇHÖ§Èò‘LÚ>K¹ÏP{ğï²{Ç9¹ÑU# ÙP»×»G±Oª§ÌóVìîUØ™mo³¯ê6ÄÄ·óÅÂ¬İ*
NœÂÀ\Èv•¥*ÍrŸ™Ü/0³l|b«6cqD‘ÉdF²¦I›9²˜N $—g/³/7ù¹\¹U¼ÜVWZş¥O…—9¢éD3x§÷µ?€Î(šZ<¯oŸÕŞÕ¹¤uZO>™oÓ<ê¢Eû¯EÏob¯	“Ä×ÈqØÎËk7øJ|°D|ú†QlŞªî¯6˜h\>P¯ÖP©ë!¼m°®¸…“š'‚À T•¾ŠuzÚRóâq<S±¿.WÓ~
ï&BÕQ[}Ùnsd€T(0eŠ æû’½j¯J´¯‰¨/¬±]ƒV/0CÕzéi£<4‘á?Şû(jLD]‘Uho/¶Şi™“¶×7Ş÷>ºÄØ1fÌË@'‰2	lÍSŒ‘=Ï4î[Jqo­äl™ÎG†iš5{ŒÍà}²Ş_qù-pÏÎÏ{ÙõÚ«¯ì(4ƒ9
@ú   ÿÿì][“£8²ş+µ/½Ü±½§/Ó»½§g¦¢«gçÑ!@”Ù¶ÁƒqõtÄùñGq1H	Ù€?tU52ˆÔ—©ÌT^¸rzŞãĞ)Î_	€jwÜ İ¡«vl=r<“l{„YC­öOhÙRè?Ä‡ôîPüµŞ£?ŸÈ÷I!Ÿˆ^Y·…Oãî¯½ûKñŸ?„‡Ÿvûôû«¿qÜÔ¸/ƒá(éˆôÒØ÷ 
<İ‘éèáª 6,PÔÂ9«ÀLÁB?„¸€ª×q Ò!s)ôÆ)Ç	få¾”3çÓ›í6Ûù¥¨ĞÛ·(©İKh÷İ…º“¢5ü zeè“ò8r–ÒXÙŠ«Rj˜M3K¿Iƒ9®;eìÀd}t/ÿè0]UµU˜\i½¸7ø”×ÎYÛlîæ'}ØÅ#Œ‘,ù=¤ÈÄŠãÇPtJ•‡Ã4Å^Fëä„Ğ)ñæ
®s¸r_®‰$¯ßĞZ$'4—R•ÄÑVªP
ÀëRĞäf <Z±”,ÅN¾|¶Ç±«.Jï^§ˆWnk”•ª+®ue9š¾vàğ•}îâÿ†íË™nW&¡,†çºÓy<Ä¿Q0gkÊç=ÍZ’ÂQ"£ó~"-êëñÿ;øèÅ‰”˜Q6ô<¡18uÍ:Ï¨uÆö„âz(<VÕÍÂ­‚Y\xÀ´™ÎØW„?W_}>¾Ë 1…Œ‚"ñ;fPÀÊy¨´T†VMà;Eñ°]”¡ùâ¿gÆÈ7¬ÁÅı9n\ÅÕV!×ÿ­¶æR¼ Ót=½)KñÏv“ëzÊõ¯…O›LdË‰²5Í2h5D¥éIãcªNÅMI¾¹¢j<MD¬®|Ğ–nB•â¬
,X•‹Õ‹ì·Ü> âl’¹›zéZ+Xïëı“€¸ñÑEÙ¾|zßM÷wU“šw¸‰=ôüñÂ[ï@^½‹w»8Â=ç$…%+PVCµ³öI¦®íÀŸ“w–ôËÈ6W6Ì@:_¾T—õÍåqOÒài¯Ì`e¸¸‘¯z¿ÉÆËß
ÏÆGT‹0 ?¿C÷AR5 Ûw,G:³¡(+Ëi*Fh4ßrºÌ8õ±¬Ìö‡ªiüô´…$¡&Œ¤¤`¬tj¦Pvï>Œ"f C™õC¬Üñ±wÃ¿|%.ê~ç&Œ&Ù´ÚIg$U/ˆHFºîªÀt!ÌÒ=İÙ÷â­O×Ï$´e¯ÙoƒÆo{Å¼òF»¸—š2ªXõ¾ÙsÙŠIÃdğ„xºµå:†°‚&*T/o,Ã²šøšåî·1 ¾5ªãzšËß²s8>zˆË1›Õ\!Ããb¾ŒËWWô£Ã&N‰Ó0=€É$ETÅ´UÊÖ|‘~Eãƒræñ <xäMll;lh¸3˜€ä C#P Î\NoOŞ®°kCcˆY ’HŒêw ğL“r
Æš,¡ùW<ãªß•„ÏÃzş¡;"E,Â4‘s¶ª*+¸r¸¢2{„
¤ñ~Í(—†/Ÿn¾¿.ú…R.“Ê•3§$»0šÈŞ\®gÑk}®Œ ×“ƒ?â•’òçW¨;›ÎŞşbÿê
›Ü'<ôQ=‹rüI£•)5tšn`ğ·"î/5ÆgÔ“Ô†¬,ôÿĞ=¾/,[|®À²bÀŸâÜ~”‚÷•¢C”]²ªéûÕ4õø¹ïIÃ‚åzHì¡	¿‰?·i(Í™¦ºíø+¡(üë4‹â N¾ÄgOä˜ù+Öb@Âô°Î<ÚÌ/éÆÕÏ©S›Q†aô´Í}¯ïvxóçKjuæÓŒrlKcê¥½)=ùÂ¸¯±/ÏÑë±˜šç6×n>sÍ«ÌÃ¼šlé9½¥=ªä÷ê3¬\ÛšgWšHwßìšÖ"`/®Ü•¡›”ºj‹’²()wt%eQ?õcQ?õcQ?èôìV?ø«3I­¦º‚¦¡CIšZNûÊqÎ8-ÌWy,ŸÄn¾@°‚Bñ;´·c´#æÕınGğúÂe]ÆŞëŠ€¡ŞçkÎñí¾„‹oÁí;Š»á]é?q*§ÚŠ¥Ú¬B¡Îe•5÷0{}ïó3“‰&wÍœ™.Ğ(¬©ÅUí¿
ª%o·h]‘ZHI— K"’1} CVXWÒ™¹G^ÓÏÖÈt¯˜ò°`¶#ºé±å‚@é¯Y9µs æBÅìqÜ;¾HDTğ6 xû[G/(ÑµÃoŸ?IIsU5OR3¾[öAò•W‡}Ì“g÷ »IœÆ!Ú=çŠ³u/ÓU5Ì|_åk,Šs\Yg»¿qã£”h>,eåğçKÈ<‘) v®V¿Hek0Qïa Ú÷`Âçği#Y¦mY®ÁŸ	Fv½“‰¹Ùë$û«vÇÿùõ&	²]ğŠ<YKlí?ášÊÿ¿ãÄr‚ZM @Å¢¤¬±¶ÑÓÚ‹ıI¤P jÀ:Ş‡A y·Tº¢¤¸,œTT’SW±]}Ó©ŒI¼[#Ùt@ë°@jDHÑo[_Yøû„~ÄW¤äBÇ±t—"°*Ç]şÖ¯+$NÖ·'T«®€lA×é–ªk:ê‹¬Ög(Ô$Y»¾éªŠGë6åİpÆ¦é…¶¿GˆÛr¼‹Q*)5Yu]/Ğ™)ƒµ°"œHœ•á`eòI-lu¶>|·)®ˆd?oNöÕNšj«<]!(ß™ÂŒ®¤GPÔÑ%7fCr"3:‡ Y<ğ#<¤¿%Û7ÇtC1æóùqˆIo;†ç9´ A©g’‰İcšÆíê#»ü ¹Fk°ß¯cd¦†Q/ŸA>×ôÔŸ;×-îœ÷U¿?]Œ‹ôF§‡\óÈú¨MZ¢U,h¼SÊ<²Š>ö{ #¼®ı€“Å’<ÙË‰&kô@¡)¬İ8Ù«¯³P|JqÉ2Úi¸Şoâ®£ãÎ=«ê\[Öí@êmšû"èF8f:ŸŒl#ŒãûÆ}‹€EÀ}P­T§tÅÿö’°>ôp˜ SÄ
×>5İBfáù
ĞİBgÕH™¯3ºá¤\›ñ½Ş¦aš¢[Ş)e¬Œ x®ªSÚRM/´æ%œs»“HGQ	EJ*SµV–Â¸ÔĞû”·qßá-ÏJ|T”R;Ïw]Å uªì X½Æv
Ÿ°'­ÄDˆ/€M?ºwù¢Ã÷/„Dİ¶M…R¹ø–ø‚Â\.ƒ?n!Râ.@ÓPsUÊ¹ŞåH&ÕçO!#9QúÌ"É½ï3ø‡:…‘M]ÿó™…}÷cF¤÷zÕ@WyºY'J¯9aHİ“÷¸¿Àz·"=Ğ·ı‡,Ìöì¬;'‰y£dÆ4‰¼ÁOÍÅMuCú´ñ'ã'"Qj7­Ë”E^²öëÏx²
Æûºâ¯l_¨¨š>1¯áê¡i¼N²
…]Ã•ü-¨ğ>§Ç¢$\ÛÅ«Şcğ¥¸@c„,Wª‡•ï°›yf]@QÎ1föJexÓv]Ï¥e|±>O‡I?Ô'ó„ÏÀû~·ÏJ>şñp€İ²ôŸ˜r˜JO~£MòØ%-,ğ‡	fùãb¸Š)#Ã·¼ÒÓ“h4chşx©ÕMÛ6]JÙ8q1ÚOj —pø@¿/U2rÈ&Ne¢Hr“©N±
ÍÉR>¥Ğ‰¬ÃZAGéÈHÑ¦zØ"!ş¢}TN*›L=İàª)ÏaÇü&zË½úïaºù)JÃ4Äyøzİç1?üGfdg_zE$¡øcÎÌb®,ÔOİåÔ%9C~zû…r‚õ™=‰i¤bTŸ}NL9t_d‹I§İ6êúábŸCoÓ.	wN–ùµ¯ˆH4˜†¦(\>´ëH´ğù‡¢\îÉ;NI®Õ©?¶t«Ïe‘qÕg‘q“qe_<\§—Û£Ô¨hÅFˆH4C×lËcö´Mm™M¯¼¯(˜n&¸E¾oqéšÇ6¨k^)X
£ıR‚ÅõW£ Ù9sgOÂí"€4Ä§IHõ}y¯C‹:ÓSÈB\iŠkø”\ ibïœ­Õxƒ‘¬¬¥³èL:‹v(’yÃa>•`á¡…‡&´3sÇ
ıİÿ„ğ[¶H2b=E34•Áöz!âî(Æeâ4$Ù«ØQÃâ£™Â©áŒCğ­‘x–+à†âèª­h÷`¼¶ÌÕÂ6¡íSâu¯]8
­îõ’³24ƒ]Á¯D^_)á–êê!ÛP HaJxFcÂİ.ñ!]_eiæŠuùzRûC)y¢Rr’öyVwÇX²P=]Î¦‚7*+AÿÂ-p·Pîd®fÚ”2áÂü'[ÍÓ^×¾!~TyuM÷k–Ëp³^á;.ñØSdxÊ‚_Ë¦j`èB‚ ‡ô½ªÃPÚYëeX\µ}˜G “«ñĞ&ÂíğÖ
yRÒ,ìØ"XÈ©£+5]¨eÁ?GWÖè5Å!µvq±Ï™=ëhì$‰J´´Ğ±ÿI5ü“Q¹–y×2¦©XÎïãíñõn±ÁûMìÿx}w<ô®¶KI§£èº©áô9PÃ©C…ÒÂ©wâN
gÌGZJ¸Ô|ê2-2—C	%aº±ÿæÜŒ¾Bß…OGgüÍsû‚íy¿“K®áÚÜ›"…çI§JE%QÚ©æå¾u^,5*L³3ÚkâçÌÃˆ´ê}×B.¹%`½ïWH«3÷Ãb­×ıÆŞÏ;JOîR°iÓèÌN$gÒ”†ïô\yÈİzpãÜ~&ß³ô6aœoZ9¼À~ßñªûÎ ;ÀU×ó”ÿÁ{·PX“mAİV¸êíŠ¹íòöÅ¤Õ­L—…#q–ÏŸ«¡Ñ!˜òµc¥í(¶@vÄ!mGõîeç¸{+Ÿ¡£ĞGŸ>~,Cø :@Å¿¾RË0E+?]cPe·¼¯Í™³—­ôÕ	®\ğêäÙô2Wõ!bÅ­êwà/iuúü±ZêHëiºT8;£—Ö¡9+ıô„ ÿ¸Ë)q÷wêß¢È_E‰óK5OAzÓ{Ùâ¾a{6E|{`xÊ†3Ô9¸+ÏŸĞo)z¶ì%šãù†Fñ]Î§ÓT%¥¦ÙhŠkáÙÉX}É¥x«o Å „i±u†iÓ =aS€Òœ«Fˆ:<*úùâ]8æ±éÃ¯‰/Øié
)Iİ”`¯8{êDã½æŒŞs\Ñlƒ¿nı Aÿú*i\A_qÀáàN ›fåÿ&áæŠ/–UF*ÓğŸá~Ê)†­i¾ïë
E3V§ñ|Ê$¹Ê<\¾dJeãÒ4ùhî)–W©Ù?±†·ùä-R†ïÃƒw<à†îíZ8âÑÉ†å¬´€èÇ!DÆç¾Sã¿A§…ùñ†‚à¤4JZé*pş>6ÛéºØ<ÆGâœUó«ˆşÚz	û=¢}½Ùn)­E.“ĞªXH×Z«%¡õå$´vÛ,ËÕNo_|N,Òƒ~ús'_8Jêcô¦rÌ) ¹&ĞJ¦`÷QlŸã¯”SØPåÜŸaãt™\õğõ6Œ¾òÅˆŒ©¨ÁöBŞ¢°Ğï|\>3„Àh¨8é.„×eœInX\Ã÷5ã=¨ÒFŠGQ_V`òûn¿çŒ>>;—”j“i®¬,ª¤6 Ê7è‡Â›ë{@V\ºnn'H¥<Iÿ0lGâÓ¤#U1á!]Gú”’)¤aº¥Äq.ÜráO»ŞÆ×Íˆø3T{®AöB‘ùsjĞzŞ/Ãò”{„dô®˜‹yP	·üœ#ëBÆ€i9öÊõù›·ĞTúñÏKqJñè¿˜»ÌŞì–Í».øR@&YéÃYé¶Ü§D¢ÓMºa5>têôÀ™D-‚Ü:®xSI »L&É	tñÅğ}ƒ+é©¡‹SÔğbÄáè¼$ÌÖhëRR­
rín'Zµi“Q8Am>~4¯†¹òKGU‰1>÷x£¹ê{§7u:»{Õşàô¥±İ}5¾?@äªÛ„>ÄRñßq}Îa"ÅQÀ´U!Ç	Øï“øyÈ± ¦çDLˆ™Waê,ÅR]\*•®`ø¿Ùn 'ÉG±ÍÀ€\ù CY`b¦Íşl‰­‰?ÄÉ7ø”²æBµ4ÅV.³f‰TF¾o	•ûóZayÌîmä¯(2sNº¬ç:óÊÒKŞ/ÜWÇ†Âq¤ÿğ#â7RvÏ|?pø6×"¦ĞÖà0…ãÅøÄr’_¢Ã5	#ûòå“”ƒP[f@Ëë¯—×Ğ™®f.F‡šæË0Ì•e)E®¯@h*mPdÙ)Ø53nÎ¢¨1Ö2^ÂÁÃ&à;°İ³2ÂÜÏ…†"TÑ%?-ŸXhñ¼âcäÏ=»©ÓÁˆW€[éãÂ·ŞW‚¸Üa+%‘G ê”KÄ–³G­7LB:/jÄx ÉúsÅÅh­^2a¦Ò£#)¥x:ÿ¹svùC¸…¸$ £Ä0ôC°&õ‰'¶¹ÂŒGÂ‹4 «§ZIñ2%Ûøäl
í è†rj§Ù–¦8”ÖøÍ43Â•ìfV˜lëìÇ…ùkdÔ™ÂÉ¾œÍê%€º­–Ú-‘Ë¥mÍVS¾›‚D¾ôvİtƒßm¢|ìÎÙ2‘Ùºd¶ğ2[ß³”@¡è“SB)“+šÜ:ÀD}ÙÀ”zòa«š¢yã·JYM)Åÿö9À˜æZàOö
ı®û5gxNÓÆÜî³èÇı­|ô·ñ“Éå(ª‹øH4»=²ò×`¿_#j²šÛã‹°™;t)JV“Ê¢²*?©%U©¡œ˜×TL-‹i8F!ºØnÙQ‰·	ŸÛ†CuÖˆz!:¼Nå[¿Fï”×å‚¬®6ãƒxÎŠ\‡c°\t¾*dù’¦Jx>3ë× 4Ì’ÿ)ò’ï{ìÆ> º¦b®<Ã¡”×@1RËGfç6lâtj¹{j|¾8™Õ\ä.'ölô€h®U1C|VQ*¢ËêæJ1][h@¸=úp¢Š,'S;VıLQ;æÛ]Q/dÎa®°îûÍ…Éí¨zİöïdøÓ.şox=~q4×0C¨ËáÂ/¿ŒÈ/;|ÍkNÿ¡ûú2”Û3Å
¸ú¾WB’Ó6.×œ( Qö+}Pş2“rÒİ´qœîv}YÕ
*ÆR]=T¬·û¥Òë¼÷3âE9õ‘±/}£°k6>óu¾¨„À´z]ğ°Ö…Ù„>=„Mz{>Ñ¿Sâ÷{D#r²u<Mór°—G°F>ÚNI›…ÃšŞAš([„C§€ÿ9oí±ÔUèÖÈzˆÊÙfí${RW¶®¯VºP¼ã¾±	°ùÒ¾\y=?!ÕiÔ1–Ò“-ŸÎ“¯¨¯&^{imCê†²¦ÜÀªM9‚šfÏ‚7Ûíµı^n¿è‡"Ğ¬•NIpš&6n´/Œ„~±wÜ!=ñí÷¡I‰ÈÕm¨%¦‹ôˆ>l€fZ­-ßë€HÅTäÂ\§ß÷“På
²İª~ÇùÊtnm‡Së«9¨PôŠõŒ¨ø=DàŸáoÉVN))ßSU¥à’ ,L¦a)dHj4¸¨hJã=„ûOaôõcÄR€@ß[™gBm@:‰İ,#@ıío˜Æâ6!ğqù~‰p.0€îšçóJ2Ëº#‘›İfi÷"ãD¿Z„¾~®bDº¾xEp›cU"L=OUG§Á¨ø±wäñ/CBá±`•§óµ*F…HPk,öÂ¥U-Z¬^î/2µ3/Š ¹HÍ)ræá	<·)zö˜êÄüËßOhûìtàîÇŒì[¼jÀ©Tëo~n@÷d²÷w¸ì°Fnİt¡,o,Á@v‡³¯Ò9s²6*u–aL–Èüì¤Š{q[úäñ'c)âs¬İôc­WñˆÒs­˜‡
T´Îèr¸Q*qÓÖ­•i>K·ì'
h1½)ìß7Ø?‹µ‘aPÓqØãì¤8÷Ø:­$è6FØæís:0ÅŸËHÙ'˜RtVñ¤)İ_¹:­Ãå¥µÒ¥U÷¼·{ÄÚ^O1 Ê‘DõÑ"ÔjåAßñ)%·zA•2‹á6Eò8È~P-}îö´³<fzç9
]Ñ87Â"9Ä]òºÏ‹O2¸’êÕ(Çz­åaK  ×ò<VáÎ¡ª9“yÎ¿Éué3y!tI5>ï{’ı.Cå«Àr]Š)ÙHÂ%Ïœ²FÓì2[qÍL^¤Ÿ|8f•ñ3ù##³c¥[€Vá‘.è¦L¹"„CqÁÄC_ÎÊÂÊB‰£*ŠµR(Uw`¬›ÄÀ÷ÀİùtŸÀS÷ÌAœ¬óúBç†w¬ !Ò5•~¸ñ±}ß¼ÿ„ñCŒ_ñ	Æë=şyßO,©Zušn×K¹åëDò–hã‹æ-!Èñ5ı¾†Kï™÷9Xe¥ÒÚ‘R†e0ï™JBïºT2XÂÕàO„Ç„ÒİÜ5§yÅ«ƒ²bJQŒÛ`g ¥ˆaàØÀ·¸Ò0û,§]š#òwÊ_åsø´AÆ|Ş 4Éş`‰Å,Mo.’vC“œæGû&ü©/÷Ğ&9İ>ËcüÈcÄ/XRJ)òbY>ô„İv§ZÉÂãİ0¶Pnd=lâTJj jĞ\1«öä¸®P>¡=şwAØÕG˜ìò­:\oÒ¸Ú¯¬©
´\_£9á‘bG2£)ÀìŞR¤X±¢ ËSxe"L7UÃÕ}ÑøÀa7†°ÿ"ë±í–"Ç8Uğ¼ë'J9¼Ñ×Ô¯wLKsmW<z¡=ş GˆÏ2ı›­[ 8ÂYª‰ #ÌçK,ªã:°Dµ¤ÆŞ]_g;Úd+ß6ëÁôôñ¬_Öğ‡yl—-Ú°@b‰°ôUUÈêi°ˆ¬¹ˆ,ø'>? á{8¥SJ]K€)İvÙ9¥leq=İ$Ì‚~}2Í¦‘e†– §|™!‚i?WpvœŠ‘%å;Ë×ùèöMÅC$H‰ığ)ÊJ}H‰®1TÛ7GTèf¹C9İ5«)z1tŸÄ¸"umàø¼4sÉŞÁ<íÅáã£æŠ]š£x#Š²3=è?d#)AiÍúÊØ@
vş`¿UtëÍ¡(’ïëmì4kqÂ“9H×ôæz$Ééq1šäÙA^¼Û£0ı¾¦=%ÇU˜ßÍ98®+Œ£Zn>f«a€ã‹Úı	0øƒ@NÑÂÍáÜçMoñïRJ;;´<ßV/šF¢òY“ğ7û£ŒXv±+ŸQñ'¬ØÅv¢Or:géºï†M9¢+óÈªŞ_½Lñ?=Ã*#f¬‰9T7QrÁğ5¾Ÿåè5<¨×Âˆv­–õ3>Öá søl¹òâöÒ#½§ _äÍ	6åF÷Ñ‹·3l9äÏ>©ARËhMaËX€;¡‰KŠ!Aj}M`{À…ƒTE’ïª¶YS`÷u–Zş@áÁzG)ŒäÔÂ†À5}O§ÄšŞ^õâ%Ë¤H¬â¸ 8-…¿ÛU€í1´¡%ßãˆÑï¾Ãn:õĞfn—tXúõÕÇ×<¬äZŞì!>È»æf0Z^ĞO,œb‚'¦ÁrDD¦B=Oô^'egì{2ŠYÇ’\?oc4®d–	Ønãoë=ı5"O2	_ÀÍ”ÕèÇCƒãô	ö$Z4#˜Ç4(‹dØn|³†ç¡5D¬ŠséÎˆÊš&çÈÉ” 'hèÖ?Ç¾”0Pèû†Ñt	|ËD7õ'!f¾Ş/ÉRğó}Œ€—†eŸ”SU¡­«pAÂ§Tå»ÎÓ‹Å—£GH—K¿ÄÿIB­”*Üš£xÖŒŠ¼ÁU¡ı9„ß1Ùã¤Œº‘ÒÑr n”X8Òk$‘ï&bÏÍÙÛP[5)É+
´'4q!©Ù‡åAÛä?ê(Ùxš–J
¶Èwc,<Z»¸qCüÙjËÑ­1Ğ¨ßÈqÑ®lzPÍLÌY–è­=…óC·H“Yc/_»9g|ö8uÔ`ºãéÍ–7z¸ÒN×’/v¦¹ÀåÌ>lSœš‚Fµƒ,.fESk	¹˜…š}vÙxìQ­è°‰“t§CÒP´$»K7Ç{æ\Ñ7<=ü˜Âİëò¨ªWØA1©8H¿dÆWµ%–/v£üÎÛµÔÔµ”³\Å×³PQ…ğAî²ÿb±Ìè„‚7zNLïëÙ,øg’QJË­òs!-?$ñN’ÄV+K	LV¨`)yÈDa Y1ÎŞ@şŞŠ¤§-ñ!Äï-.kù†zÃ¥1
ßsOæ³`i,,Ño]¬‹(è€_ôàEO–#lËÔè²jM0¢ Z¨¢)E‹D»$
{Eqâ–UÔ"ç¬8È)ÊlSw4nïS?¬öQ÷³öìÒÚ'#'ç¹º÷ÑŸùÑÎç¸ ïŠŸÌççĞ-€ÉÑcO4)‰iê+èRò8;²SW!vpf^Ñ6lª½‹ãÍÿºö²ßY·ù
¿Ë¯/|:Ÿ^­Äe†‰ìYÃ×HuzøÊ²f±+@®ïOÕWõV@J¿ı÷qbU<oŠSù5ÃNwêòˆf¹¨³W+ÅĞƒuãkŞÖ²/j‚i¿LıÄ°Ë0a	Ãzà·õbÁNÀR[é±5¸‡âw™©g¾âšP(ÊrfTö
÷luíJúÌoMÓK< ™];©Ğ
@XšÃHrğâãÙ&²•Øx?Ãú¯ñiWÏrzUßxÚ»]¥g<íÁõ~ñ4¼Ö)ØÑ+òõ¾}â©O–Ö#^nÛt|Î™ã<¡tDo0D‘vô’û[¸úñÇ
ÍB$dci:ÒşªMDñ“#è}}Ä§µ¿Hª­Ù†«[:%Ò÷±ı0şæ2oÅ¸ßÁ;Ÿıø„ÄG*†ÏT(Á½S3À)”€~I‹Šˆe½ßwY‘‰¬›köû‡8©7±'uÆjEŠŒé+Õ°}Z+ÌïÇÑÕƒhXö“f\ùˆ¢ÿ~üõ—»tw½ŞÇÚª¿Şõê‚ÕzP;“¬˜/Ğ¥NQª•åwŸ
2ÂgèÁp/ÅÇ¤+ÛW=V©Ê¹ÄµŸf®x–[ÚŠ;çB×3z&NBúÿ8fÑÇ(Ò¡Èµ<;P5¡ÒÂğÌ[í!}÷&ê/¢¦ Xéı“©ÎÄ]ÕQÑ’^EŒcJrš•+…sèÊˆ¸ öİÔ´€‡=2‡Z€&@s§õb¢
'àÖ˜\gBÌ›p¿G¿¬ãnÌçâ¼CÖ0úh¸óªßá&á~v¹g|Æ,×ºÜRæÜP¥kµİ„\×Ô¤(vC—­ÑùÙçÕ€=”"}8T¬2H¥ÜÀíYœû½D³Ûö3Pi‰¡;a*ÿjkRWq®¶Z÷¬6/ò¸U›ßíëSm?sšÕ¬åÛ‰?•pÑÔÅ ™¦€Û*;+ı‚¾G˜¦è&R7ìÀ‡¦bp) e^Yî€_{ñ6ÆQÒQ¸Ãzdkü[p€ÙÄï\ôÛ:3Z©ğ<‘—İ: >¦nüçúì8J-¿Ó‰öJËĞù;ØnÀ¡ßĞo{ü[k`9¦\•r,ÔÍşëœà¡,ëUDå¹uáÓ¾Ì#~Úßî+€hÏ¦*-¾g:vA· oÊ§ß®X}¡ä(†fxfNtZáO«xŞ‹/ŸŒÂÂ<ë§tÑu¼–h¯}¬˜6ºŞWÊôôS#s%¥Ì°”^hª-!w™ÙJÚ¨ÿ  ÿÿì=k“Û6’ßïW(ş°kWfÇ$%QRrñ•×ö$©³³^7©«Û”Š"AÏ”¨ÔŒ§Êùï‡'Ÿ HBĞŒÆV2–D<F¿€nÅyàœ‰°’ÑS.gu—[ui‹ñ¤™âÚ«TãB”¥u­#ù„}QíUtd÷<ü­·ïcïö—~ùÇÔÛf$¯‚–ãUaL­€³­YİhJ‰œ¯L]Â¶ uk¼ß§»$ğ=ş÷</<	E}ïû4âtQz+X¥Gá4sP@Â&©;-b¤İ6¸ñM”ûW£ÚŒpø€ïaØÙA×²‹£0Ÿ‚M´ßTëïZ-ÉepTÔkAkME›=+ˆOíWB is¨åWI 4 ÙhQ3 —ûUæ§`}ÀV[ÕÙkàİx·:1KZÔè%ÊÒŠ¦KˆE[Zû˜ìö;]ĞáÆôƒ·„2¤íh…³hUódk£ÉZ{º€|¹Ï¯4q#ÖÔ Ğê
!×ƒ!%81ÈßNÏ`ó*d°PØW
=w6µëªø!"Â÷ûİ:õfx¿>
¶Àgc5“‘AJÛ'¤>vl›Óa¨dŸ±Ñ6•H¾¢ƒÔ]Ûg!İ/Ó'Oš¿OS°õÛ—k[ĞZÉBÈ|Oj6ÂŞ3è!o­Awr’[9‡§cHêÜoí´=®wé]ë;Gå8³`îL8ç¨î÷ôRÆù÷A3êCÜ¤Zç4˜ƒ™ås2¤uËoÙ¹VæTN»=äÀ‡Á\™†aÂÍÍQ$s•ıİÛ~zå¥:˜˜-‚ÙLrÊc¿YFØp«
*òÎ0`d$ØÛ®wÿ	‘Ç[øù=ü¬%?®·šúÓ9Ç` ¤:^ú	Ï¤¸êˆéà_GaFğ5…ÜÈ>€:HSZ¹8À`ls´÷6q4
TyQÑ(İQÏèİ»§+º±öñíëŞ„t6bË°Fúğè=ÍFZœaMá”Y.YÄé‹ùÄó¦râD}‹,K1õb=.M6Ëkf'’˜ôQğ>4]êäËÑÄ*“x Äİëº®dnYa0D–ã«P"CÌir?ÕiÖ1Ë®7]¸S‹“T=İ?¡pæÜğ!t&q€BÆAáøÌò]©Í'³Àò8>¦ö=¤[À‹	ßy²šÙ?^"ÌÈQÅ½"%‘äudBµ¤KôÅt>¹µÀ§8üÀ©‚£§0ìŠháùó·İ{q|;"×äroÒU’'ï½üê‚]Ç{ô+şEÅééØ“`b·¤êe”]¯Ñ¿GŞ‡Éowı£WĞƒ.lìS–÷Ù‰*İ }GøÿÔvÓF	_]^¯1:`,?­ü üd³KA–=Å½?;Qº£˜C(•”*±E3ŒSñnåÒ’Yl²pW–ÏÙ@¯	¼²×#‰6.s}®É¥ÖD;„/ğ¦×à„e\œİId·çjl÷’µÑ-¼ú}ã[ø}Ÿf„£5I²uöÊ@†FŠ’†Íön¨¤=DÉ"CšPÓjªXÒÂÃ}côd‰å¬kÉÒ1·ı™5æäîŞ‰Ü‘SK®¥œğCÉ%»ªŒçN˜T¥¿*k*ÂÊZ}ÙPµŸÓd>ƒf5é ç-D°(F…¦³0÷ÍB:öŒ›ˆRŠ_Rº8óùszßï;Dh¤^šs˜L#¬ V§›ÙĞK´±×VmöA”Œ<ôw¹ßBÆÇOÜ‘ˆejğ‚ )ÈË"SD›÷ Ke„­˜Ö»Aò>A½­Aû%úı
RìFäW(£ï!-³ÓŸÜæiÈÈ’åÑj!ÙQ’Ü‹‹ü(ü~ğ4’$d¢"Ò;B)E¯wirâ[L®ÁvxøÖJF¨®p®‹åì6‰.ÌÊ¾yq^\ÓO£&d@@ñQºVî„÷y´´ämvÂy÷ğœ¾k3ù¹¯Qr„5ïÎU†Ò«d–ì°¿L¥™àæÖÂ¯ÛQàÚ÷R]‰ÎJşVï‘ãú¬vğ_ùœ#›O¸HáêoÏ&Â24n£„¿ìó<†’2VÎe7¸jš|Fy©o—Û$ÂÈ÷p¦–.Ó½˜]lÊqÅe—D±€(8HÂ™;ñÇçètë-½ô~öryKD²<~ Î´W¹OÛËØK× ¶YqGÉlãÅqGIâ
£ÅÅó‹‹ğm
VqE]nV*ö_*ÍgÙ˜v¿ËD<±äTl½Áİt1ñ‚Ê/Ì¹µØËsÏ¿ÂŞ)(Hı=œğÂ5„Q&¸ÀM&Òpk2´¿‚ÛWåHŠÓaà×Ïki
ÿ³>;wìxc·_¨Ê¦Ã¿¶4¤˜$Tâh¬Ï¾»rçcËŞ=V—H¨ÑdªØùÜŸL=ÇŞ9šê£O¦S{<™ï˜¦¯#ã¶UºŞt2ÆÊı«u»ç“p² ÑšZ×cÈ× ¡+ĞÙ›Í.W
C›†şÊuæ
hÆ²S‘¨W+Ç
æsÅN)e¹JÁ{°÷ÙÂÀzôş®Ùõde°×B‚¨wªvO—ô! Ì¶³šõ¡ÿ_±æˆñ?³ÕĞ¿ZL,ßu¸9´øŞBåB­Û™?ö×Z(wK©Î^¨õï ßšÇª8WëÔsgâ,X
¶şÔ¥‡ãX“á3;Q½ïp,x
Ü¥èOµ;Wì?XÙËRèÿGNªÁ~}ÎüÉÄ¨HËú˜gµş§`8áXau½J¶P1§İÏ•x
OñâÜjB(4ÖÜk€©hKpIR‚¥¡@t!©Ú”ĞX>I$œ\£1ıàQ5wµÂY´ª`= jH3ê3¦öÎ´‚èÎt¬`/"šW/kFH“×
Ô‚¢Š;…Üge)èz&¯*ŠÇÁÜÓqóFÜ¹ªî3]ù!$©©ªÂy€¯¤©× \çÍÿkE'§&’¬Ã!*šÒ
šmÛº@ƒM)Ò¬`2S° …¦h pU\{t§…1QE'W„¶åWÊıë j 8ÿ´ QÜ.fK…ÜOLÍ@êq®Ä¹ˆÖç…¿²fV àYn 0UtÅø«Ğ™,&
«¾f¬ÚcEF7³Æ³˜¨ËH‹[rêä0¢¨44(XíkåWusªêÿT>h…”}–öñÙıå/´©óêöš·% ÆÑ¦öm0w,a«CÁqy[$!j¿z³õÓÛÊ™Ì=5PÎƒLì—¢!Ñ”ñ:ÿPv"´èÍ –¢kACÑu»?G ®åR3o¨¶~©·Æ‚ÛUã
£,SÀœ	øL•õ{ïFp).r‚Şm ÜÓY¶¼ò²+Ac•½[Eñ6‚æĞ«ŞÔ}¾‰6äŠlÅI )ëó¡)Şk¡¦ZwOpGÏ7»É“áU´˜¡ø>ìèUïÉ¯ö›UvîÏø5‡òEóë/…Ä‡ßuµT;]U6$8tÅÊ2}™çi´Úç€œ?óê_[«º^œ·Æë-œßFwÓYS´8«§Q3Ø§ø¨“±ôuÿµÍš'ó^ïƒ‚B¼ÖÚã/VétYªMJxÒ¹ZèÂ”
ëêÙÓûÖ5HêÂ¿:-a]…ö a]kÈëº°®#ç@a]kìpa]k®SX×JAXóÛ?‚°Æ=OÖk}Âº;_X³Ø[e‹OçŒ° .	½ş–Gî¸Â9FÆ¿[#8Lú×)æn¥?4¨Kü^*ıq	$®R(‰íZAË³¿ª&Û¸É,¶¦<>ÒJ`/«¶Ï¦B¥„*Ø~F4´ojçQF—Ò
¾¡.‚yFÒ$A
à³RÿÜ{q”Ó[¼±SHë­C`6cœ¸l‡<°Xô·ª¶á)¤9
ÑìÃS¯ÔÄ#Âiäpà¼Å²p%P¥^]¤Ôj›ˆ{ë0~»ƒˆühçms^´^rÎÁ“WÛdË;^Ô[&érÅï*BË0Ú®AºK#Îqtvàù*Âñ4×†HÚUûh^£Ø>¿Z"LX°ä!,Ec.ä°«R^$,£mw™dŸËÁYFå¶‰¼Ø&ß¥h§–íÄš!LrÂ‚ÙU’æ¸DØÒGaÒÑğÊKG†'|F¡çkĞ9!¨a’šc‰W„¼Ùp™	X6é°+´‡Z£%ü¤Î@PïI[@xo¶ê¹i ª@|(.¢V˜ÏæSgÖóT¸ wº;ªzŞ›º>èQáÀí
×™Ùsoîh¼2®eö¤çŠúà•'İ[Í€oy=·‰jp ¼
gÖ8ğzıÍ‹Ğ]yâÚn8*+íu”¡˜ÅbºÅú6°ı™?éyŠ„‚*¡­œébÚó-ûjıNæ¡,&*³^¬1ÅÛ¡L|•éÖ{÷OMp|áúÛT¶+Z8ÕÎ…úw-t¾§ÂÍQ^q\(N>*P¶ñ;(S3tånG²¼FğK|‹ÃRTD¨œ£àó.J©ªÒ|O‡ÿ’\ƒInÃl¢á•Ô*jƒ˜AüpÖc½qZ¯ÊKÔqD.O'/JÀN¡â(……(z'¨ÚU…ä·/F¢<ıBÛYD6q•âÁ?€]Œï%İïà‚€Ÿ—ü…‹/Ç*0ûÒÈÛ/)ü¨vJşå“Kå|b)¢£˜— H7“pâÊ‹Ğg´2ÿ
ûX2£1?t¿¼‹*\Q²a‡Y´â¤O(4Y4¤«ˆ“ş¹€9ÚòŒ¢íâ&ø	ÒZè7Áok²)ÙÁøı>‹¶ÈÊ¬ÌÔ-í”'Øš%+€Ç†BÒNÇı®Ód¿ƒìƒ)¿a=Rœ–Ø•,ˆP¥.+fÉö¾‹ş´Lño½–K+ É:ğÇ>ò?-	°mçïÅHaù·p¦·‰p‘”·²\ƒ4ïˆÌOÂ0†)|O¶ÿ Œ\*HeÛAùÂËU‚ò|B|¿ºş§úä£O\<@ÀÁ#E!„Ö-dC1­R¿K°é]â ü6	í×k¬Z¼‡D4ÊØ·¥tãvëeÍËjä‚¬+Œ; 	‡F¶ˆ0Xn•ı²Ê[¼*qââ½$+.”zÛ6z?¾ığşÕ9µk¤k¡ô`K´^¹«åØ½>ÿùWL€„µIHâ]ÙIx¿Æ’;—!9gäe[yˆ3WXş'/»zçíèEg´ş‹ÎL“Iú2’²¸äa—Ä1‰€/z—´€É"> z’Ü•ø^Ü9vÜ"ì>NÖ‡{ĞÄº’ÛÑ¦Ë›S)ù.ŠãH2æ»tÍ±uvå!—ôyWGÄŠµJ¬ÒSğÇÏÈ…¬>î*“A…ûH…+9Œpâ1»{,Qöñ
©e€2mØà[İ‹¤N}´ëüòi•zP¦^•Š(áRE”E¦ƒ\ÛµdSÕHøKÒß¼ì="cI=:I­Ñ`XÚøÎ
9,Fˆû˜tW‚L7‡fÚoÄß0ı»Ò|%­Û`ØeÍÊ¯ªÿ:€ô>²Z•ı,ÓˆPiı$§à/küU\{‚ÿ‰ô)¬a^RmŠ«buùËÓ4I_Æqr¡…òàûTúÜô©PØXÄ#Çµ|qòæˆªrêh‘ã%©u(M^öéÛ\»+°©¿Üúùï²5Y¦#&Y(KÔÿä}÷ˆıd>‚Íy²˜Uû*ë¥Ñ5RWê†°õªôÑ<mì´Ò2Ú°ƒ¹Î,¡;•êº.Az"“’8àú"™«wæ­ìÅÂĞ2j’aÌ¶Æ–k¡SĞ$7èHá'æâ5™¾ènTŒ n”†)Êğº°‚ ğíAxínÕ·+0™
w„­ò£Y«‹…ì©Ğ+luÂƒ)"=×õm¡CY@²&}Ë]-\Kè±•,¯ó1˜`*Âã47;¤–dV¡7na	pÁ¡f~óI¸XMÄÁ®ÂI³-ÙJ[Xë{óÁX±˜±;3{Èà/ˆ/‚l\Ê;'î\œl¥«iYËîÌ],VCÉWÊÂyàZ+aÌ£œ‹ËÈË+×[b£ÇÏ…0“‹„7ÊLßî á@·eœ!œ:À]Íz1FâDe­Ÿ·;’q‰éÜã…ßKhtuÄÏÓÁ"é}0áŞóø¢ŒïM×_=q·øQx}¢î8d k¹.Çé•ÜbÀHa‹Á3Ñ˜©÷©İ+Îx<bFØ§v¯tƒàáfè­~ìƒèÇšê†Ç’®Ü®u¿Xh†¶x<ºñ[<¹nz†-Ê4®	XÌÂ1GOîğLû„ÎšĞ™î[<í6;ˆÀ\íóå6_½²™ÂLYZ {áÙá£fÊu>gî†;ÈÊ¨·.QôDU†s0A3C{v¬Áº‰ ™ş=Ö¡Ì,°fs{â2c ıÂEZ Boçò¦Zg}Ëã_]9Êz6¹Üâ’¥›`gÏš¾Ìª“Rgş	z>L¦S	ÌƒÿsdUª§¨KäÓì«ˆC E—{q.e±w­é­Shmhë4TÜ‘Tn,Ki;ÃÉM/9æÏ­hÜ"ÄÃËŞÑ;Èİ•‰ú†„W5¢|«1ÃØûó¬_Yšº©ğâTôù	Á‚|ƒvş•GqöD[B}qç¥³û"/üèËnìé×u¬jll69´±jammih3ßî3nk˜€¨fhéé“¿ÙO8œåi²Ï!XìŒh•rÙò¬üDKÓğ"ôÊâı¹	­]üT6X-ÕX^°ĞSÊ¤ LÅ²‚Ÿ«§\~Ÿ¡ßˆÂ#âyEmÔs±0D<ÄA¬Å`»†{6z1r«Øô¯¼ôeşÔy†@øëò¯¢¶ĞCN¾P^ÉÇ¼àqÊzsÈ>¹OÎX·‚*íøc6Ú5›6ˆ¨ÖÀ üzZAÂÉï¾ı¦¸ˆdãE[|ÄbU(ij*w˜}Ô§¡eŠ ¨³Jõ`sô”ÇAĞ–8Z$D0Ğc ¸?ñŒÕ»¬-6tˆiIÎÌ ¶~xÂ"½Úê…s¢ÑÛ®„z0<ÂğCÙÓH¨ÕbeE6Va¹Ñ'A÷xUĞôåK7bØY´tıïïÜìâ®–ÿûü¯ÉİxCAĞU #Ãf5¶˜»iªúgìá¯íêƒ.Âä9òäÁªîoøÇo¿íkÉÁ2B·÷{Å?ôÀ1{®OÃi_ÖÉ!cDş7NIB ä¥ØĞÔ¯^¼Ecö€ÆĞÀDËÉAi›²]î İÂÏËnºo>yz;°F9z|ƒ„âçm >Ãÿ¼ÍÁ¤çøø%üò´xÁoøpBäYOZ¨>q5¤½÷üOçÈ+*y~Aª2Qg%è»üZ±øŒÈÇ«4¹ñV1Eëm’â¼:İ‹¬lFbËˆŠ8/[Ö€±ôµ»Tw‰ºÂ< ÎU~?‡Ä!ÌÖX}ĞRkh¼kµ°_d´àOMúÄP?.ı¤Ê‡-~ÛkŞEr»Mün»Ôªú7,Iê¢¿ŒŞ½¹¼|ùã›%¾köâ·×Ï
íœÈoŞ,ÔÎƒ·îŒã©[|oLšäÀÏA@ll|meE¸kè%Še#÷;¾MPœ
´__nƒ6ıdù›íu”&[’òCœ›£	v«Ägø¶İ‡G:Mg!väˆH!s´Q¨æÿUıñ;®®uèšÖe—‚¦fš‘‚R
Éul¬:PuäüÍŠ•Ö±}€~… Ñ6ñ×K @wAd˜¸0Z¨ßA@¾=ùòÃ—'ğßâæ~ô~ƒj1rhOÒÚíšÕË•©=ª6é[´æë“Éq®”Sù¬5±#ÁÌÊùã0Õs˜ºÉÇ,ãÀo_şÏ›üŠU3µÆ‰Ï ıÂéÀU{e`S¶¿0^8Q"µåÀy_YÄ¬ïp+%b„ßıÔÔ4~ëlôüG2HbüÒiğõCnÃŸÀíËü©'‘iuU»Ğõ¨pä6z˜AG³U’ãê¤ˆ0èÏsŸ´ ù>³ağƒ¼Ä6‹uÔr×h—+ÀƒÔµfwTM+”¸N¶[ÕŞÈ_ü/èŒn%¡A­@;¯m®ä0•í.ÄÃŠóÂß‹·jŞc=ì¢d¾r+Úncpé.—`s©ØüÈ²ÚP}Û[İ4ü¾£lmò‡Nn1§l“sø\²SÏGK–§µ÷t›|ºop{!õl„×§ÅeŒ%© ğŞsM7qã©i¡½¨³$
4gäicvE+OÎy{œtHb|U°&_<Õ!_¤¯V‹V:µÕDÒ™Àüy¨F;‹ü’v£d‘ˆEP~Ò*_¦eT™ÄšGP¿d±µ66UÄnB38÷†„¤æ€5¨ ©{tBDÈíËuİç§——Ë‹ÿx·üùõéØbe€¼Mçgt¨cŸ¡å0 Âä¬\ªMÏj‹ãX¬¸]™¦ÏšOë}Yr»DÒ\ªíÓä ¾Îœz®¤‘@ÆScæ'ÅŸIê'<ã&%¡œ2‚²M`ôh€s®’1PNTª¨ÈŠ?y«é’C„‡š¹ª”EV+­*q/m›c™?Kß¢.Ç‚M’Z«I”T²pñˆ„Õ>/D¦íJJ±’½œ~cª3”ç„Ú‡u†R×‚‹Çõ¿½«8fu’ƒ¨vnSä{Éó˜ñ<G™Å2 y}5ß«Ü>ğyOŞM|_]5‘¾Ğ)WbŸS«¿+H6)®„¸I"AØ{%Çàİ¯øÂJÅ_’Äç<°(Q‘ä‹ß}Wƒı`NÄ7KqŠEıLÈòÑÀR.¡G>iTRâ
§ã¯8}5^
áøŒêRC*MÎğÄİµç†¯T©\å¨¯©¡¦ñ÷-Éú¨."l”*F­¥İ™HÇvÚËrãì›ğO<+(zg¦[êí/Ò¨kK1&ëtŠ©öbÁ¥S6µ»(¤•NÄÛ+e”´§æ9¦Ã]
î\Ke1:5—ÂÃõÏÿPËœ)¨4nâÆ8-Tœ=¢!ËÁsƒû±ã1Gá¹w'…hœaƒs/7™šñvoÇCv<<Ü=Ç»rVÈªŒ!újj˜qtG‡qtGÇI;:ô»&3MîšîØ¸îçxš±÷OÌŞo$Hff?µÅ|GÀ	n¾c/»íuµ¬.Ìğ.Ó­VíH[ÆÇa|ÆÇÁoÖø8NßÇa<'}Êù+³æ±ÆÔ”úµ„¾µ‡&˜™ª†`¼	Æ›ğø¼	ZÍ|tıŸ]>dl|cãßØø<\ÖªÇÆ7·1¸Û1¸Ámncpƒ[<4ÁÌlp¸?®Œ	|ç&pıâS™Øå«Æ¾§\,ƒM0ÙÓ/K“÷*˜ÆV”²½ôsÒ¨X@â›öF_„‰gx¯Ş½yıóK­Ñ\Òm*0“Œü@td¡Ö+`^İpH4B5ûy~zë|½µ¸:…-bæZÖjœœ;ı¤ƒãáa‚ÍP>Ø},=oÔ¢©z¥ÒøTçUˆ‚üœ&FwÑVŒN§9„mÕ6ÕhŞm¯Aš/™ÔâÖ™ñ8œ3ˆ‚Â0¶­A;2º†J–»4ñáZ‚Ë*0Û å‹Ôo¢açEÁ2Û¯× Ë¡’cyÑe+-¬8…ó »'¾ãVoÎòİìâhO(ÚÏÁ7Œ1Xõdy¶ôĞå@çÉº‚P;0¨óÔÛ~¤Ş{ª,ËÊ»F$w¼¼±x«xøª;9” EŞfØår*ˆ“ãõmóWŞz™”ååKû!šÎ¸w­½·ãivÒL2³Év¢›l
½U4[Í:b¾EĞ^a[èÆ±»—t_Oæ6i§|ú_§É~U"­B•M=ŸÖ`ó8gpmòéO÷ ‹ñ•±Êı4Â@-a·¶O$âCYş.®™K8|*EÂeûHjÃŸ¾?ö‘ÿiI8{i5÷÷šC}©šÂNÃT@>£
!eúWÀGæÏÇ·Kôõúz‡”Ê ä“ œ	8	Ë ÄÑ5Ho—{(¤bİœË‘ŠylÑ×òQ_€îéáó¯ºv{É~x¿ßÃ$‰ve©ë- Í	í‹Ö‘nÙ~Õ°[lÂ,co»ŞUÇûö/÷‘µ,õP¸’¢ëóğG*3S§¼•rÛ"Rï(;ˆ=?kø‡‡T^œQ7ñ€JD~á-R;‡TrÏ*¾ã!gg5²ÒVéO®öàTˆ¸Uƒh¹‘µØ`àÌ°ÒÊüLì”ÔĞ‚ßPÎßâ#Õ9«xMUF„V^c*5‡Ãñ_ñ›“sì3„OÙäFçŒ±íX×ê;çlš„q'Ç"{xÁ5£şäòAÍS5æPî˜ôSwUÙ­¡r³Z_æ@éc<PzpuI‡ÏQ3z¨Éµ:
ÛÜ®tÅ;¨Y.ñ“p!µı:İvmê(y:}uí~Ş;º6¥—İQ¼rš¨Dîbk£QìtÓéWkc²íi;3M3j{³~¾«L¯7¬Í6¹²£¹ÀÔp«?!¡mkÊV0™:¾Ü©9^Yu½=îã•&ß‚¹øÑä4ªx jôü¹¿ÏòdÓªeNV™“U_ÁÒË9¢Ó9¢c6”Í†òĞå~û´ÚwÖÚXvs¹…ÉÅa¶Núwa67ëú?‚SÎšhrÊMÂÅj2ŒSî«uÊ›ñ±›ñ±(ãc3>6ãccŸŒóê±:¯ŒÊ8¡ŒÊ8¡Œê¸N(İ—É¸z<AÎ
¬Bo~9t%I@ëx}4éu¿‚´²Ê>•wº¨&æÕUÁ^½Âs!)d.d1s!‹ñ¶|=GLLúgE‹ö¨6¬¤~©è1÷”³tYªÛô™ê1}Bk6·'şü¤MŸ÷›ÈØD¼ZÆ&26Ñ›èQÜ£"#c‚Ä˜ Æ9dQaÌˆ~fäL×‘OÕNæƒ‚–P°+|Ë]-\kvÿvÅ×b#ï$®9%«¢Á#Ÿ¬ßk?éÔlI" RM}‰¿ë¥Qrò	ªÇs©—šA–Šæ[÷NS$Š?¶\kìœ@zã„3N8ã„3N8ã„3N8úÉèÆÙeœ]ÆÙEİûØc=*´7óVöbaÚ¨ĞF…×5*4¿¶Q¡¨
mT£ õ„Tİ
££Gaœº3w±XÂhÆ»Tà7‚ß~M‚ÿ+kì ”nZá,ÂyàZ«Õ‘eÜãcâD†Vªşâ¨ûÌQß
…-Ú/Üœ‘—½Ş„g»Íİ[Ez²ä»:âpA®™€3ÕŞ;*Jx8qçÖä´øÓ`ªN‡o®Ó/§ì“3|Z‘O_üÆµÛ†™tÆLøJÌ„CÄQ?‡Ô0gÔW(ûN_ÁÈO•TEX¹ŞÊ;-‘iDÅÁ*½‘F.V]™ÓûbÕğSkÚdÀOxlnİc“áqn¤İJ¡I@ ç˜åŠ×l½Í°ÉÒz¶fè©šb:õ¶[\uš8ø¼G˜Üì/qŠ@fß
Ôë=CCÉHÈM®r¡ãrëSÂAt2‰vÇ÷‘É˜¨œ$¦ë|Q=Fõ4ª§Q=êÙ¤Q=Ø¯‚ÑøàôÀcègüMæ0TUÑ.LîU£ÖÕ‹µî«TëŒ&a4‰×$–^ĞvxŞêG›¹zîÑvoœ”h;^ÊÀS¼†[·Ü»ï+·;íâw-ƒøxÃ7gÌkDwòRQ
¡ÆLƒÒ*'vİ*ÒJyO4…$Ëµh½ô…şè9‚PˆÿwÊ{3onYÀ:¹ã4Fès„>“\¶ôR°„ıdÑ*‚ÚFcĞ’tF–Õ»Áòxàhró?^uF:§ük‹‹ÕaªîĞ_ï~zşİÇĞ„†”—F‰b®£é=hMOV	ÖáË£-aèzï¬x ú©*·Ô‹,Ïã:)”0:òw¨cÍ±xIÈÕkÁ8pæÓ‰etl£c×JÛ¨±F5j¬Qcûu:=RYìÖ.x×K°[»²æÓ¹ë¥ò(•F/4záİé…Fßê¬ğøô-£+5‡£+‚öbë½Ëvì…g‡î©i/÷ÌõãRu¾¶›oÍİw©=²*ópOõ§>úõ½§{Æ‘ÎË¡¿ÏŸ³#pPEùúäŠXãdô‘4çè<Ş
¶
Á¢ÊÏkRºTtş±ú?à£8´&¿@:Fî÷Í7L'ÙE[¸¸„¯É
Â×Æ	Ë\Gàf&é~³ô2ÆàÛ="y„‰¤õA™';V•ûƒmWÉgÌçÚ…êÍRÇt££ò“ı6— Ì'ë.Yú;‹î’8^^'9•Eˆú%É£ğöäy´…\d‹¿.3úÛü.oÿş:õÂœ‘\€¾p«†I4n©¯NM¡¯·^ÇÉv•æÍf¿ò[Qhó"f-¸ï!Õûû,O6Üa3Âx¤˜fÙoÄÅmFÙ[¥dY4®‘¼§‹Œ®*Äò^•"ñiÃ,hÉÓ›(÷¯FO%EĞã{ı¼Rú»VYô¤ ß§[ÿNjñ¥z»ô\€3Vë ¨Ş·'{²8¤'X½»'2İİ4*ŒJõ»ö;¯CôçŒT`=Øà£ƒ	º ~ÅÖ„~ŠêMpëâŒUİÇ±Lk¬x]9•Ù­ÒïaÈR€ê]¼¦4d×Yk}UÎYÑu­[¾n\«ÀCÔå$ÿp/ÀÅ«ùâbvqF5¬cÌ¯Ãu`I.¨Ñ4¾ùr]PÙmÂğJÒm„VÔ„!˜jiƒ+×”Šá®"êêÕU…š‰BM…eHÅ…œõØµœ»ğSğ·r¡Ö¤î.fÓàv…õ/ØYU)»ûc.¾ËŠi€ú‘ñø¶yœ3ÊšÔŸU9ô€Šî™€Q+yĞÒTV¯pçÎ²-–Ü¯FwV)™ïÿ  ÿÿì=ksÛ8’ßïW¨òa+WåÛ%Q}øÖ“ÛÙJ2¹ØsóakKÅhóB‰Z’JÖî¿ ¾@¢A$Q6=UIÆÄ³_èn4º9°qUâÊöª‰YÙnuÙÚ,>õ­À"_ŠºåŞHXˆ$‹5’ëhZ5#Œ½Èr»ª®®¹´ÆK§wXƒê:¨®ÇT]½³üôNægĞ;½óõèƒşøJõGc¶Ò¢?ZKäºcÏ|eúã VA÷2T@şbuĞÿıoĞÿ„úß »ºÛ »5Î¢UwË¾°^¶kLg¯µá©R¸d’÷õlôÓ>ÉrHD5í·JÍÓÆ¹É(?|½Çk¥^mRàDç¡Ü/VDQÆ ­óJªÔtÀbºt‰º„œJÑ³¤Ü?!¢CÙ ñ©ÍY%Õi…½å˜"²LCXÔby3^-.;,Š^å&i¨tJëòåÊ‰Z+o‡+Ä¾`#í™ğûwOV‚éÓº¿³0™‹‚ğFrpÜ	o:`ûåüTc, ¯ÃıêÿV®Ÿ_cmùrk¦u.*Ëì’ÏaeÎ'Ëù±‚íÌXUKü„&¿zs÷şãû‡÷woúÃ<<‰W[FrÈË|»Sƒõí–¨UÇæÊ¶&îr|ì”oíîÅfñP
Ä)ù3cP†a½ı6K|µA›0z…¼Æ&Øï‚+$ıã?@D@ïTñoOÇ3k>1xüÓ‡Jô¨ƒ~Ûáup_?ãI¿£Ûçİî=›!6şgÙ¢Ùº™Cò+ŠwxGŠK¿Åí‡²DÀy¯(Ë‘;ío©†–%¶ŞRlÁÿË/ßQù.AáEˆà/ÜGŠßr2À½uıØ²ô7„ß,µykèúòbNĞ0´–3{LFµ-ŸG®½š¨Î¥ ^?¢äù†M<éš–éN-€'ïi±Ÿ–x„pË1åY¬ô>õ×
ğä°y	ŒwŸÕ2Ë0ĞÊ¢Š‡=KV7A “²ì¥5¶¨:ØOÊa¶ÿˆ§3I0‡ÿ­ÿ¨óüwÑÂ/¦®Öó¿øš„‰Ğ6üËÔAI¸<%iÉ vĞ*ŠÏ´ŠŒ×±ğÿÙ„Õu°ùd†æKc	°9å l¦¡cÑt&Aö³CÏ‹Ìô6c€;™Ó³p	ÇéÖ.•ŸsLt÷§èjmFQ×íXÂ¤úÎİê"ÕéÊôæ+(ôD8ìïÿÈO–oH ÿ^}fËÀv©ÄY—ô7Qd=¿-ñÓz6Ÿ6±¸¦ÿĞH +{²0gİN Y‹ısâ$ot~zÌªÈ?|¼“¦Ã+zm·úW¬Ïşá~4Z6¯àëP!Ià…´˜s+Ãu¦Şp(ŠÊ {I€!©FuzIÎYŒ'æ
"9JêZä@z¥<*ĞŞæ‹æÔÂÙHëoŒ£±ÌUºñ7˜Ïw|ƒNÒQ‘5>à™°„š’I6	©³#Û!œcæâIáÉÏ¡x$iÜYÍJbMaöÿ“Z¶£?äyŠª=ë1˜˜/M	”®H)ùÈYº®+	E©œTVí”;‹½
‚£½ªÔ¨3d’®ÊvrWÓqäôİIÛ ;bÄÆUQ\o”¡¦@œ5İE'…úà+>wwÕ’_f"Dúl©‰yMÀu W¹´Qˆ¶NôLkı=K´ò¿s¨rùÑ“?1
Êu:ùSª¦IñÊ†×oÿ]x^± >æIÅÎSœN<µ¨¤ZÄÖÑÜ2–®)Ê¹—ÏR9Í<à(Ël¼»oü§l6ÏX:®5›)O‡­Â¯Èõ#L½
3ƒi «(b€zÀ9›ş)Í•ò~IÆ’ó˜Té5_Ä	H6ŸJÕZ«•çX3O‰œtİ(!k‰fsÔeÆ¯™{í3B.rB¿¹ô$\·XœñÖp¬n·äÈ<rtİeëkƒ"4’T„ôZeB}Ô›†#SoE´óTœúË.ˆµùŠ=@uÄa¦…ƒ«ƒ{sÔK1[ÓÒ5qfn0™5©QªÂ7¥¡ª"‹ÉOÕ€= ·ÿ]µsÕÀÍ<úìûíûÅ¶¥=Æ%g}ª]üïµx0¸êcş{ŸÎ\]—¦i®fKˆ‘ò[‹¨¤%ƒ¸ÏxÍ>aŞb­ô)X…ŞÇFÖÖ¥õvÚİP.¦/®ÕÏ•É(`´ûpËVFƒc@KlNFÓû ˜¦w‘cA!)~|Í)Uæ³·Éé„– *÷¾RŸê@pÒpè“ïâ3ŸöÖø¦£"b>íƒÄÿD*Ši¹C·Òÿx¶¬¸o]Te’ë“Æğá
Çâ¥Bê–ô²¢"+t¾’ÔAˆ³™g-,°û{âÔ9œd#Ù“ìuœN÷YucÁ×w6]Y€Q:0öe2öÀ~#ıìGü"+r¾ÒÛ¨øKûé‹~Q½ÎÂ›x’A|*S~××,?“'„iö"ˆ53Z±˜qô/®U¾)¬÷Ø—wU™	aìRùZÿá+J„8!²„Ô-¿cJ^‡§Ê¼mìò€Ä¢}?¡íş6LòœÚc6ïJ¥E—ûµÕÔ±‹IS¸hu“D‹V§dƒE+_TbE+eCEk³õ3RT97†²CôAv²qı+Ló’ÅOa”¬·Vı¶ì¾9•d3h÷Wpò3îrNôã&GI'	ËëL)ÊÚcOKDÉzÎ)4ˆ±ùD§s––5q\@Œ]§á6l2?8Ãq]r¥Wı«kËÁÊ«¨k=ëàÉøšĞäf¹…¹o
* z"¹½—bm
èAt(ô2®*¤¦ĞsrR\ïÅVI‚'t…|»9P¸¹«±»t=à
­İEš‡°KÂFÊÅ^Ù§ğÇÚßfÀ Yé÷­mcl•·´Ì[¬]?Æ ÷7¡[m;RZâ¾¥ÄH²†K^]We™Ê$~®¹%q2œdÂ“ 9É;
úÍêıÄä(Á<›V-°©§pI˜ãŸÕµ`«áäîÑÉİR¥`4…fW<¿)t7¯šØ®/*ˆ»©+,UîÑ¡¾Ô^ÑÜ€JãLîƒ§Hì#:ôñK®mÉ=Eiô*å?Ío_rÛµÃ„…-İñÕMæüë>ól¬÷½F—[ú§"¹ƒ—'z¯u<&µ×¦ÒFëá.—–ét €øs˜|
]ßó•Ş\1“OÙtl8ã.“ë¥º*xúÊKCØZŒ;u§HX'Q~ÔA¡oÿ9]5&˜­}›i×DQWµõ~Å@¾íc’2÷ô¶Ù¡Š]‘mPA—Võs4ŒM`uÊ<¯+Ö²è’¡>F€,Û–B­k\údÿ~¤wÌ:b,ËõseóTKL±màoQ6Y*1uòE©Z
öw’ÛHÁÜìµ$ÜDå~Aö¢R4?o,ÈÕ(Hçc½—$zRxÉF)è“+_S‡Aîá‘Q—4/.R²pğjğ99‘•‘¹q7şö«ÿø„O%²âµE~±èod:¾±Ï-ãû&3€nà¼‘Fûğ+™kÇLH°:$)·êsi20¤£êÈ„X'§Éƒt1 ø‘H4èÁË!ƒèYÎmZ.şvøšŠ‘4Á‰ğª…ÏtV>’^{¬FÀG5È j,Ä\‘Ôu'Ó›’z€-Â¡pÙEhãásÔÃXı*H©¸3Ş`í%-^ĞQJÌgàê4Û¹Oø–›Ä<òXé² ‡	ÜYÎINŸÃ3€"1ÙXlqÜ¾³õµÉŒ›õiÀ#ƒM9–OK£icygåçÈ Ì¥órmMS‘¼¥Ìt5É[_µ»VıÒ."àíT™;‘8ât×ÔR¬i«:M¹ÕÂ±Ôu¥ù„‹í¹ìÍY S£ßı.ÿ•š(æéA]¢O®xD©2½ªòú³«Œ•Ô»šWœ(b [ûÔù¼÷'sÀ°[Ù«
™áiÖqÅ¤"#gº½„ÈNyéiëmÕgípV–Æ{:]ÙÈ›‹îKÎv`_ìÑ;‡ÃqØ2Î‡};{†3BdÏåN)gZh¼€ê-1àµµxä<æxÿøˆ—\8î˜kUº¦ûu01[=ğp‚OŠ*œ”b]ù<à;NİsÎ OMb×ğÒI\@Ö ne	ã¬ìI'55ÍÕ*ä•k4·±å#ËJ<cî9ÎÄÜX½*Ó\İzé¬Ù„¢Wë%xtĞÄ|å™+w¾êò)‹9‚ğpBRÚ w/Ğ}HA[’×H8ÔgÖx>T0ÕI*cvÉ¨š)i¿õòkhÉLæN&·t¾vj÷ğ–«4ÀvTŠ%ñ†ˆuˆg_P9+µÑsÃ·}4ETq0;I,7+EUÿ¨?Uï+9ÅÏÙÏ˜)e5=§AÉçw%EÊ>º+éSò±ÚÅqNŞ*¡¬U3€´
ßg¡Œuï™jÚv‰SÅg^%jµªå0¢yí'G}#v#¼«Ò”9~K¥–‚‘ó•‹5h L©Äù…›55ÔšßDƒù/óÏN¸ÙYø{Ã$“ËK¨®±Çü	Ã.¢÷<YG²Îb–úÛı/ŸGÉÂ„HÛñ°ÊÆÚVâ…?]äW´B,…ñßX3nØ)Ég¸¶€¸Å³¤ã¥¨l’[D–ĞUß‚ù@›ú,®r–Pè´l¢rÂP%ÿ™‚æ|°ë£*,SZ‘™ ¾Q‚ü%—kt¬°2¡›Xj.D:ÇrÓİ·âS ïTZÇ£^??YşVãÉè¬ĞØ@À·Ô~2ùP;ÎÉ5œ8':q†#@«„†åà©$´’$ÛE!qšh”böØ]Örª1sıAªÙ'tÁÚğKõµŠtØ³ÊicüÕëF(¹“›İNKÒû™ç:¦ÊRÙDÖn³}_®ãXøØtÍ—Ê¸ÔİYæÛ»LåÆ ÓxV™Ó¹±´N)›oîN¤—70À ™_
OpñxáJ½‚49»R^·KYyïovÒ(.gÆÔš¢E'Õz4ÓwÕ„q¿ã%µ´mÍ}z¨lí§~ª'¼9äéQñ±B¤ı—Ë(Ò.î®>tøuâëöĞ«í7–'³J@:ºë:9F¢hÆÒkÒ'É”íŸT»TÙ$åMP¤’ä7İ®ĞÌp\ªºÜ;ê¸d_˜~bx<1L&öÜ™@ùAŞÙóS’8iØ…’×9¢êù.¹•U©u5µ½ù|áÅ2«’!ªÔ’šßØék„ÅuÂâCj…Å]ë…Å=®–fåáÒ-ù÷@Ñ¢ .´&õŒYÇ†Öàöårâ-æ6 ßKVvÆÌÍr?ÜDEÀ#ÈÊÑüı—'İj„+<a,=aM3Ó0€<b§a^ÑºÈƒš-éç»Ñ&~5Eî»‹S>á`çq°Ö«ú3¦·âCÊ¼ıä7å§jŞ”rRd¨ëef®óvæñC	Z«®Ô Û*‹‰17V@IÌš¡ÚS”Wœ/Ô
•T•FRóê]„‘–û-s1÷¦–4ÓşÀéHÉÜ,i–‹>½EXÎ-4‹&mÃb„N"3?*å$”<dO¶s³* JI ƒÃŠ¼k‰@X‹ér®GßÄàw¡³ßukğ	éø:%€Ã<H:ç‘Í¢â )ÑY@ôU6(+±ÊÁã9UêÔYÕtÀ‚¨kTRèP³O‚Q®v¢X|$zcEèiKÑÕE3¯•†c#åëÂ±½)ÆØy´U„³V“±í.,¹r ~¹„ŸïºU[—Îx6^(OxO
p~Æ§·ö Õ£Ö¨€® .§êú@<Á=6ğ*„ş>-»†cÈµjÉ˜uÉvë™äÄ«l¬%›6aöÙˆ
vĞDK9_¶¸$K¬•ªŠJÚÅÊtª
ªnŸ›
S€t¹à¾o(n,nJ?‹›)ò«øà‰ŸÂ˜zœÈ§xåš|y
“p´#Š-(7û¼XÔfÀ<ê [k0]k¾˜Ù’‡xú0èàú±+Ós\Ã«LªWW8šÀÁªàªÎ’=GÖ¥ò9 <TeN‚}ËĞ{t=‹"Dı`$?Tî*õ`„±R?*¢q*°Ïç=È‚#¹2ªrê‚•e	7Ã*u½m“¶#hK©·½C²QÇ!’Úáı‡¿bµµ½ŒF¶9¶\Ïìòê$şwqÎìÙ:{zH©†kÌ¾=,Øúi+Û5xˆ­à½®uU.Blªõì8ÍÈH2Û=DT’I81şœœ®CâÜb¾åÕo\µWmŒ;/’/s¥îøŠ'ûZC5§aÏ¼1
v/ª	-ë4×kÀÄ•6ş»Òß]ú’š¹ŸÂêÕÅfú4R‹@©šÌÅdeº€‘;ƒğ$"_P{që=	oq³²ŒUÿ¨ÂUõ¾²,ÅÏÙO~"˜V²"/œÉ~Á¦Î“µİ¢ ¯6ñ·ĞßâİaFÕó Ó±WæˆO£¸—Î=ÊÖ Ôî{T/â×k%èb”d>qU€Æ²R^‘}BÉSèş)ğujsœÉÜ ê<´[äÇS6˜¡åMÙãŞŠÜuSm¬’ĞiX~ÇPÛ´B¢Î  ÑôÃ@Ôê†I³n•×ëídèç;ò£æV*àpœ xRSóg
¢/´Ãål!+¦İûíÂ(AnVwñÂŞ}œè‚B»‚#ğÚmùŞ (‹(ât>¦ÖL§íêÒ™NlÏ2$,n%Xãıò©Û¦gÌæîÔğºNİuŞ…íÙ.òœÎ[Î‹0w›Ş¯cÚy×ø<î8ñÂÛh5F]g¾·6Êè–¸Eæ©û¨7Ê.¸`6u9›sÕyål`è†DÎä—	¾Öº€äÒçò B$^Xd¢ğò@QˆBÍ´‘Iè
Ç*iè;*ÊS¥Û1Õ&q|s6{~ÎïÊ¡+€éè¹ËlchËõÙjãğ¦ Îå!±ÙT_"´ñ÷›ÿò½ä]è"Ö*Ò(ÄØ`sx(H€V=¸>dælÙïƒıã!Ó‘şÒs%V4@v¶w…x¿·‹ğŠC¦‡GTÙ=¡‡C@ÆPó×İcdFÑµ¡TWğYÛØC‘%äcÉ®aÆƒI°>–*¾¢Ø
´ "Iu~¼şå»	¢:¢"NnöÉ“&Y›¥
‘»(ÜıBkWZÁJ,?8XF	†U]Ù^ 0·¾«cAåhõu€FgÕQ"ıºD8iõI~¸«ë|r,l*9‡7ãWùÂŸHVb0È?o±§.êµ-ªˆ#NçHõñ–¡'.ô#tmmÂ=å¡G.Ş~ó“§÷ÛÄO|²è¨¾Å
0”Œn*!*Y'X-É2ÁEş”‡ çx¡²nUBKŒ«ôRËèŸ‘Iz“_'¢—™·§å"¦ Pµ˜ê·7“+†ÊúM¯Rò…g”»ñQç]á¾?´ÆV—L ƒÎs©PºÎ+Ç´ÍùÒ\)ç&ƒ‰’ºi=)B‹ÜbRçË¹‹
{ğã¦{íò$—±¥µ‘İt2w,ø
ï³x²#™â’c¡ÈúÑ¹³É“Šx²KçŸ*J-äÛ$É½Tä¡pm'‰@ÎÎF"Š©D$ŠÆL¯¡ŸŠp¨JXCFóV†=a¿AŸë[¸n½!ÿ×‡PÁ&’½ĞH«
Œu=·(^%Ä}èD¾nö®j)1_¡•5Ó«ä·ÆC	%—‚éRié:]YoÛ„58jÉÕêîx1”;r«}¢.ªé‰P«œ’Ã°{Éõm4S&Ğ[ßuŒ¶E€Ä.Xõ‘ºãÙ+we˜íÏywxr¬bt$õû†§>dî„ n¡åoãõvÏ<ä[í·‰*ˆ€[xÌô´„¹éÁçÂÜAÑåáp}å'Äü*ú©­+ûÛ3"–ô¶×©ZÅï¬üÔIÍÇñ‹
øJVRçbœØÚ5¥ÀP8î…ïÀºWN—Ê €ã¶^%Öò÷“cm¿ì#oo<Œ´X‘3ÏuLd‰RáÑy¾¤ÀsïÂáïôïA£:P£JÁ¨Ëb,¨$Ëğ9ûˆpG†=ÄbÌæhe.eº|FJA5h(Q¾¬µ¿õòğ%zäo¸ûìÂ¸ğ‡a~H ¢ğ„Nz_üün'áæı&ü_¿ íOlƒ-SEywş[no÷~àb3&‡)y>v•}Ãê^åïï
”’]L€¥ãMoÉ‘t¥gÆLçÏiÂÑßòŞæy‘ –{øøõË»ßóiG?ıäĞ‘«=†‡õ…'@ÛÇ„OñÕĞƒÙşåfKÕÚ,…N«úÄ€Şõ_~ù0ÿºH´Âo+,3²Ó¿¹ì´G±âáÍúÍU²ë¡&l·tsoR à® 4 )<p0õ2oĞànÅHıQ¸a€•ÊÑş%ä†ô3CÛåÖëRÁ|Ÿbóõ>U¨väa0>D£•$”3PŸ„ğj<»y·x·Ğ"„+»Äp™n-°ó±¥©ø¿!¢ˆôæé ©S¦iÆ®õ¼½õ„¾qm<aŸ¥~úò›XUñ[^?J iı—àÌV G26¯• Ãš ¡ı¬Îj%=ÌûjNr<+z<XêêÏ‰İâıÈé^-‚¯$µà½‚Ôb÷JfPè7»*yB¡›yÅ²†¯^%	7«íèukG·©lyû&§ä7B¢†û•´ü¦°á¾=¿ÓvÓ¬“oè»aÖ¬+Dâ—®2g1ÓB´g1—ô—=š¡ş„C#°g54 ‹uñ
²ãC¼‚ƒÂ4ÀR@½¿²&|ç{ŞÏ[<GÒ'5xa,Ã˜kQƒË-²:pÁíI€¸ı¥8	^ÕÑvÉb»“ĞùŠvåôÊøvæLKñÍì0½³AÂÀ]§‘/L.Ñ9²Íu	›}qÂ¬Ş,C£×*ö„KËC¦é £KK:OÉL/nï‡ÊØ; ~ù7çæÄ1S›„Mw8èu—$
_•€»dI¥•úˆæÒ7¿ºuo¸+Ë{æŒW£ÊRæ±Q|]»B’(v~ú Œ¬>òÃÇ;éŒ«¢BU­ju<Íõ²ÏÅ¶D÷•Bòª¥ô"¾"‹æ¸ )J@RÕ~ÇŒê­Î¤-ê|n¸³ñT²‚£Ã.â>TªÁÌLŠ,$Š”LDZ™ôs¸í8©9YM–¶#™ğµ2éMÀUi˜S"õg™GÍúÉa¬ î*AuP´2Êi’™ùø×D-Êf’’šZU)vîLŠ@áiùbk"°Ÿ¡juiXì¢Øì!„B¸L¡äÜŞÓä00Ëk@.yº<}±‚ ü±NCÒú–Aˆ]›D`Ã¹Y¦åªšİò½¬ÚkBD÷æ¥C³ÉÄš‰JY4¿#¥Èª†,Ÿ_¯½è€öƒrï<••(ÓÁˆˆ¾"kÒÚ°*}®\!Ïì«Œ4a5c¦@g‹è|L³˜N›mcçk»’ñ\hAVªÇšë-ÉJõüô<¨jy4Â³ H ÿêâ%Gtß5L$záÓ›à=©ëÌõ}3ÖT«Ñ¿Dã®Ö–/ÉM~´[€¬£õ!ÜiU l{iLL[`şÆçßæ34{~­¨CÀm)øUIP•¢ UNW³¹áÚ“¨^+Qİ!ÏÂšËƒõ¨WZ¹Şj:› æ4D>ÂëÓBPı¼V"šô!Œö|Äá/ÒÔ‚)Ï+»¼|KeúcÖo	¬ ƒÛ>n	—r¢4è¢š-¿œÌdâz4eæ/¯ëEÆlÁM ¾è¡2ÁÄ0'À=”&˜LU'˜Lå& @_%On‚##àPÌÖeHS]ÊO¦1^ÙG¶g.'ir‹ßQYÃhC—¹šŒİ¹+ºF:Û-ÈæYò¦„1’-°ó·[aãúãLy#?ª?	jî[òIaÜ­0 ˜“G­:1‘H ÅìXU¹”„»\£TY+i±ö·vø¯õÆú—Bfô¢s¸OºõŞoi¬FÑŞDÉÅ°ó …{A}¢>¤»Õ~&¾÷¼Q’`ÂŒ³>ÓßŞg¿Ô5ß¹åt[ù‹g…ö“«L,)ô!Õ.¨tR¾%…[µ%¿äŸ¤Öeâˆ
ªöé
átüáÍ‘ŠDj:ü¢1ÔºNöÈõ¨œÖ.¬”‘m\-²½jò²w§59 ë‚ĞÓµé5c16\H9=è5ñ=yÊ*‘@‚ü<ù®‹¶‚ÆóAeT¦Aeb~N©25Ñê¤U7²<¨;òw^M÷ÕA;§:×’Å$“Í
æW™ˆôÆAo¼|½±	õ2í8uï ?¯6Õq=oŒ ğåAA=‚J…ÒzãÇ±8iüâ¤JmV^P·2èÊƒ®Ì0èÊ´:èÊ¯\Wnê³¸ªgU³	§FV ÛŸ~yÖ74ún{4Â:
mù±å¡ä™k­¨Ñıß1fÕS:y0$z¦ÃE™…ÅğásáÃ»»›»¥ßlí¸y~Jeµğs*–…Ÿ3	,ü
[áçŠ\åZ	ÑùE¶èÚ‡û¬ ¬Üg€Xa{p†¹ÁïC‹[U˜lÆ2nSƒ*³6µ¬1hSÓ]ëïa‚DmÅé—ŒÛÁU¯Îé\VÿJ9–k’Msİ'V”·'…'FYå‰ZŸòõEÖ6T¼=Fá~‡Üì·qÃÌ´íóeä#+rş{¢g¦*€ˆ”õ~‰\bñ¨)U&V/÷Å–¸CÎ{Ûpgùnö[h©l_”z%A›ƒ7†şŞ˜Á3xcØ~çôÆÎçÁ¡38tÁ¡38t‡Î1:Ânõ³éu;‚
«´Yô<Ó/æÆÂvq…Äª\ÛÏko/Apé®tRÈö/MçŠ)vÚß‚ôÂ1²“øºxf©ØŸä»‘4cª=I*¦kšõG®'5Ë“Xª6=‹íÓ–¥gg+Ò3:£gú+×¡¯ÌıBJĞCÜ$0fë™}•u÷‚Ã’rT˜R*3ùa¸¯aìŒY¥GÍÙ²aHÂ¿ÒãåÌÚ0ájéñv‰4ªû¬Ô¯@*Ô¤ÚB§¸0OJJ¡²­sª“mOéI¶1%–Öİí’I=±qÄë$||½×Ñr¤{ŞÂ\L|nT ½K§eÓOw´%IşÅ·6‰egØPÖ\@û"~ÌĞĞ%ÿ I–÷6CS[3‚-EúdsyÜ[ßññÃÇøöùç;-y1½¹·rV@B,&/¦‘ÄáàQ©eí¯G¾+™ëŒù?X`^ÙV-£Ü< ¸Tò½eJ´Eê°¨/wÈˆÌ34Ï˜y»¹$„G¦:Eæ"‡ÊÖb2~’g6^Lç3 Ùw»‡\?!DØ€Øî@mèì2ßF“1Slõ$L1k´ä¿T1Tò>²ÆI9Ç1HrÒ4BRB‘¼g,°Ô¡8XÕæVjôd«×Ør˜vUCñ"iñ–{üù±tè¢»šm@4Ié¢)Á§ë¹G	/]Júp _j²bò£¬væ€×õj›%;È8‰Â_·v:ß0Ö0EëËg¹ÓÕÊAÀ!%E„dH›®°D÷zÍtè]‰-BÔÁñk&*õ$$œ™r ¿y•vJ*Ç¯SL_­˜W$ôêê~vÎæÛ¥h:Ìç“Ó¡24ÇŞdfŒĞ^)2äúÚ÷º=0Pâ™Ä XÛ«·¤NÇT"S—Şİ~Ç'ÎMh•šÖdfºĞ½#L­¹ˆÜ:H±›mâßï,-ÎëùÒ›¢?vs^ŸŸn^¯Â&ğO+*l¤SNa¬ F_ÂØ§®Rhi­ùJZÕ‘!7ñãPƒéüvAŠ†ÃÙ+ÂŸıµMâ¿¦è:Dš5G‹%T¿vi—Fkˆ4¦—ï|»'è	£âB[N¦æØ\ Ê¹Äã>äú–ğ«·‚Ø‰ô
ï9AÿÇ(²¶nöVëüôyÉ‚°ÅÑLñ¤æe.‘w°§ù#›z$ŞøîÊqQ â¯~ŒÁğüğğQK5;d›Æl&xáºÃè
=Fù¶)\08
-ÌÍ¥½0–@©Óãï×­ïññ/ƒXn£:9›Ú³¹=«1vyÍ/ê$÷Ñü´ìÅ4O
7Ô\gÙ«j`Ö~ŞY§X½”ĞJİb1Æ,s&_Ñ?÷(NK#¢JNú„’§ĞıS¦Œ_w‘éxK)pD¢fqn×>eæÈ¶v;lØÿS”²ÀŞ'I¸…¾‚!_QÙÓş%,˜”Á:3Jıy`ÏEœWT%`ë 7^!…´ÌpĞàº¯@FñYÁ²ÑË5îPu„ÛÄr’ØøáŞ¥ÿû~Óãñ–ss6 è,
ZºIä²Û¼DîTÂ¬¿9féxl. Õ;;2SğNnµ.Éõ¶™p¡pí¦qÜĞ›ûı#V>’/Qèùúò&aAŸØ†]î¨.šOMÀC¥Wm¤‹şóˆ.ş¥T–¡›ÒuƒWÁøçğCı°"7~ npmè¾ı°¸›› l‘,Bß×ôEºĞ÷ˆ•¹¼Åq‰¦XJÕŞH]¾¢Tùâ$ûœ›ˆ8_v¹ë¶–ÅVõZfÕh£µéûÉb5½½ÑEk‚XEğ#èæÌ±Úé²$Cµ_©·El§;Ü%~.údmñ¿‰oà]D^iºÚ¸È˜ÏÇ&š²–æ6´Ã¼âÑJüé$²‰¤ÎMfµ¶x•éú5¨`ÈıÆÉÍn…˜d´á¡…e˜Ğ»»5ú_ZgX(m+°¶Z'a¸Â"WO:656$sèë‰'äîAò¼(ş=ÉÆßlè;÷]ä;ÇV”p2I	\ ä«‘çazªĞWJ'çğ˜Â£* ”›Î¼Bi"u©sór’s©œo9Aª9íjt©ĞyrU'8Øå×4Æì*C¤ª·ğ .Lèê>t²·
³èæ‡£f"JŸ›$±œ§Oh»ÇÊÈM€IF£2‚haz+À;¨MÖ“­ş?   ÿÿ ¶¯ï4xœì][“Û6–~Ï¯P^¦œ-mGÔR²ñŒİvR²Ç]¶3ó°µ¥‚H°›±D*eOïNæ·/À;‰
 A‰RCcâsùÎÁÁÁ`ıv‡õÆµ_|×ìqà¢û¿ø“ÿ10Ú>{w‡ğcúïö+¢Á>úöİàÿ¾~ñ¿Ş|Ü¿ñÂÉø™å{äVèßı˜•ıã›øÏo
½ïC’ÿX´ß>½]mñ~îñ+t}ïşı€÷!¶ï0Vôˆc¾à†Ø³/Œ±Ü•ãzh3p½pPåà§ÁèŸ6ÍÉtfÿøMµê§·îno¢fwä¶@´ˆdì; íşÙ·xÃÁÚ÷7ydŞÑqW×s}Cß[‘J×–6/m^ëÇR%:.R>éÍ§·6ÎöõYÚ{MƒÃ¯q×<‹;øià6›êˆé/Àá!ğÊõş`†¸¿A¶µU"p=OB”pÙl€U¾aÇ•A©gş®X¤EOŒ™±ÀË5K°´t6É«__z|ëîÃÿ¢ÃyïiÑÃ_ß?û®¯œ@ûwL×áb:«~(1Lü—C™ÉlÙ¹FËÑ†4oPøWßõ°ıwİúÛíÁsÃGe4:}±xùóëÅˆ%¥ïİ“âI‡vJmÅ®‚›OA‚ëQg!¹í»?ô÷«Ãnã#ûÖ÷Bd…wï¸|G?4ïûõo„àl76¦Ö™,’œºß3â(İ¹ıáşIşwô3#’ø•lÈà@ş€¤]Tâg2ÉiÍç/®}æ»í“ÿ&WdşA¸
Ù9ü~şHv>)ˆ·şoîj‹‚Ï‡8’.H¨x¿#‹‰k‰¼²äTëíÂöGÿá ‡RÓ}ä‡ˆ€(óâğgò×gÑÿ?¿}ñËj2L)H¢Òt‘ÕQ¹Õ©”JN0ĞEYhÏaô„·(­Š¶ü‡P?ØOD®j;ƒ×ôUÄSÏÊ<Õ¨¯R%”ŸaDEkR—ÏD´¾¦ıPÈ	PÍë”9Q-óç½xØ„ƒßWˆı«Zƒ×kj¡=&ŞD6Ûø)@y7J™ƒÓ<«¬sú[“©}f?%MQÏ’ììo~øÎ·]ÇÅ¶D¿e‚Ëäfqw²õ¼‰v~˜a8 ñŸuz3ô”ï£¼FN7ŒÀS=ÔoI™çDZ‡-öÂ9ÚL ClôW-k'Ğ9e*¯ZE}t	X¥Y– )ñ˜H	¦¼Ì!–Ş¸¨¬äyxCq²B|ëoü@2¶MæÍ±ã«]ŒÔaüzz)ˆÌ„ù	5²>ßşÁ³¥	c§­¿F;°¿F°j‹›Ú~&{İ~Gké<7ÌÅ0pY!Patsò/¤¡İ-Úl†
6Ğ9–‰±pàoWÈŞºÈœ€İ¹
£jÀGZ‡2óH°LõnøğÚİĞÅûAâïy‡\;õEX|ÏN/YfM»Ä®LgŠ lµİ›B%œY­®`²ƒkŒ3;…d™hõÚæäOjÁÖiE\I9…6!UTŸP&“IÂIÑYÃ‰ªmÄ¥-Ëå2ˆúctÕš‘Y#•Ê–bTPBi¢Z—m‘L3İ8’X=ËÒBs5›Ÿ&ìWdOèaÂ?ˆøºC;¬5/Y7FÍk?dÍÛ´@€¿à€õ7G’±p}Î'ù•ümGÿÆÌÊ|Äaèz„n÷É_¸@cînÄ)IÈyˆc:i$QDO”éO¯3º;‰ó6!ÜSx¤„°œ€²â£®H@½ÜøÖç‚G£…hZN­ùÌ2çMDÓö‘^? `EJ.FÏ<çĞûÆİºaÄÉKRîeÉhXİÃFâ{¬Ï<Û^´Ç²Ş=Ûo7şÿLÆMé_¬ÑÜÍà„¿êFõk2•!ÓZG°¨ÿ·r¤-ò¼Û<~ÃÄ%ıĞ†V¡¿ÚîïyÎŒĞßñ>çx,kˆªU¨(Yi¢¦¿üGÖWdMLìÈ6{|ÔÁ©ÏDTÒÆÕ|Ç!R‚¥d(}ŞúOÀÉaËíŒÊ›Gş
ÚşŠĞè/Z?†ø¿ÿg°ó7›•Q0×ËSÚû.=< ¤Ø{êı âõ6oºÄ}"û…öĞ”«½U&Æ°ª„ï‰H`·ĞÕ;²é?ÓMñAEk•œt‚@(°Wq¿·O¾ï«¦Ç8Äw<(è:k«ÙF¦º1ŒÌ†cP¸£fë`ÌçÊÆ@ÚÃG*ÆÚt5 Õ[2F³Õš—[íÿõîoq€dÎ¼TÛ‚Æ`c6a}ÓŒ]Î¤ªS†JRµÁ±a,_.g¯aûªópQqô¹àƒYv?oãäå«éKóvÜñvIŸHT@©LU9GMleúhãA©ÂdÒo†4UA´ñ(å½¼*1‚o¹,“š.¿æ
AÉ8ºK >ºiÍèbCB=¡Àq’ŒÕq^J™sø'·kÔ/Œ	—åvR¹Ë—ÄŠŒÂ•	;°fC`÷¿Mî‰ûÁÌai­ò8ÃØR­òŒ´æeï©C«ù™‹W…”ë­,KV…B‘İ´¢˜ìRªİ7X²A$™Ë:…â¬ØQ.Á
ŒÛê|¥Î</Şæ…@fƒáùÒYcp7k<©ñ¤Æ“åÑi<Ù¼d`§q™Æe2=j\&ŒËÚa¥‘©+­Gæh²Xk¬¤±Rñ§±8º«ÇJ·hÜ¢qË¥à––p¢×Ëxlœ¹=ÓpBÃ‰ÂOÃ	ptmàÄÙ5µÖ°ZÃéQ©†m¥ìŒù\½²3'ØZŒ´®Óº®ø»r]§õØºÖ;'Ö;²ê ŠÌU±8[,ŒÉxÂËT¤Jô');qéE‘K	a•¹&Ë¶›Î¶EĞk9´›‡&b2“§(c¶9ãÅ¤cŠbr¡¦äEÿM%y%¤[P§L»©F¤G;›'Í™É$kÍÊ"Ë"Jl•$}KËşõ²)½º^…­YÎÊú‘ZÅ†Ë–W´ºÈp³ğâ;l»åÑ¿7	Z:¶mšé{y'¹`Ğ¥ü¿~h'ù¥é,IVPÚ»Â—´6_Xëy÷"^Ú^üâ¢ÍG2‹d^Jèò|$•–Dìd'ß„x§]J®åß|LÿùzÓ@Z€s!vş(AF–BÎl±£İœ›0b u$UÉk½ì {.MäP£ÜIM¾°ëãõhÿJ¨w'x»U¸tóuù–ÿŠOL.ÿúi`T&:À›=æVøÓOƒWkôUlUJÑ|g…¹¹µ_}Åë;EYM{†ñ€»ÿH:!í ü¡yvš÷í9ÍH<‘—¢ìò<â~Qh’]&¡ˆ×ê5Kû	|,Şá¦üVüß?GÜ7ø!ÕºU¦Á ûlŸô/ï¿à pmÜ©RJè…,]BA*erDdõ^pJÂÎïˆëÛ£t+ûTıJŸ#&[!*¸~ÛhÕEGKÚÌ0‹ß™úÅuÂ[ßÆU0Öâ¥4kº˜LMVì¤ÄLÕÎ½û£¯è‘ai¡ƒGÆën1'Y3)¶W G!v¶ö›Ã=ó­6esÿĞcºJø8ªòWºfœò3A}¢äKŞÉZÆ86z”Z«­ï…´Í ôİà
n¢ìæÏ¢–¾LF7#§
HH,ÊÿX‡ Àõ(Õı¡-9Ì"”3¸n˜ğñ‘<îBÿ¶é`ãê/:2÷p²¼Ï—áK²B¢æl˜‹ŒF@SÒìü•åîš'ôK½+m¦Ü°kÅ­i¡•øXhXEŞUÏZeeì’Äá¥Ğ$å48¢Û£Ér9Õø ?ø Ó÷Â«• Š¤â@ Á€\ˆy[Ãâ¦Å²807Ö–O Œ5èhŒz10¢}·màÇujö±ÖìÊ5»ªÈğ U}I ‰¶ê}nv¡Şí±åØëÑS8Ğê]L-i¥pMJA8BšF9á;ôHdî¼u[æ¬ôSğìü}Áã¬Íåh²‚Z]îƒ/•§«
1‘7UòÈìã§©©d¾GÛÆú‹ùvA§­¡	ÙÇ;ÈÅ3•$ê	nc62Bä	99ñİybâV8¤nÍ^{Ù\š‘o#’é0ë¨
,Ö*0æ£Ùtj—"ùª •ˆ¾·y\Ñ¿ÚÖ{+p×Q  §ğW×óÈ÷
ğê‹»w×6ğPHI€o("Ûv)ã¡MTIL'å_cJzNV‡HôôŸ»÷ÇB$:kbX‰AÖ!°ò
gó_~'»›øş»”jì—ºƒÈDĞ¬ ˆ†SsR©Ù½‚“ SÿtÀŠóÚp<„UªgG–âÛd|]®1ÔÂ——±™xÈ¹O†ºO€lÎ…K´0BlŞ·Èƒ9&æâ¢*w+dÎjŸN,ó®Bv€Ã¡2V4¡íŒ+nnÎŠÇmîæğ/ÃdR ìcˆ‚ı'wØubš/ÑÍ­5«Ä#¿§+µ›ÿò·Ãfƒ¨bÍÑ×~G¦µÚÅ“HPX·˜"[it­ˆŒ˜?³)Ê¹_Ä¬x÷vùÙ“÷{$¥r€å‚ß8âë#Â""«.…
9‘šK¼4øq<¨fRáHÈ‡"ä7rfó%NÅÆ¹qŞ¸ 9=vé1g†3Z8ıpŠÔè}í/Qã/š†o³>AOŠ´¤Ô®íz¹×K4¸ä¸Nh\çÖáã«Ú££=:üîA£
è›soş)<_Â‡®b2&e,F33¦<ö0ÍSÂC»ßÃ+ï°]óc/â2}+g,bK®hä“¢ƒªÓ$°\É‚
b¬âúJé¤Ê¢ËÕ-lFÏ”gÏua²¹­µW*K»/Z:Ûò£ƒ(ns_r'ì$BÁ6§v€L^\Û¹™œêB*uœÔW{ºXÉĞ.n¯co<²t®MÊ4`¾åb¹˜[# ‰—Ã5ÃP4·^yïc[ÓOü?2Y2ª”É€«ä9?µ¢y“VÕÙ¹)YVQFŠ×E¤´,ıG*HÕÏgÎhda >ú8Õ_	_aÉ’
M#GşJ¥tì‰™UíL¦Ø\Nƒ¥„qÄfU§d"eDœ{§µYAÄMd‹F›o)^ÌÇ3¼79z 6íÊC[V¦%\ÏÚH¡Ãî>@v]P,x¼áÔ{2[ÿ^Ğ¥ˆl¡íÊ²	Ş;ëòT"Ù}_át&|F4ÕÆÃ*í4ò ´H£N"¡•ßE¨ÈÓ_VS$V§öÂXØ3À¶8.V?c¼[ù{O[_Ù8Dî†-å'¸ğ&eè¶3İ@?Ğ]è›g&(Ó9~ª›úµ@Ä#²\È†Î¾xY»²[è¦|
·wˆrRÅ\hæÌÖ6:FŒ)˜Jînor(ú«ÎÒ9-5Ó_4«¨»Òl¯…adè8[.ˆ¾à¬Rj·ÛhÖcìÈh”ãNdZ7¾ ~Éøºc/³|Ş¹à×'ˆ}Å”Së—İÀÜ6çtH”rmÅ€t‰Fëéb½àÁ±ÅOçáéÒ½(ïQS3¬ŞÓ”í½¸2ƒcCZø±i:sH,áƒîÉkRÎ\ÁÁ_¢K½0A	a÷*ğwï;æUbª’xã‰½°_µ¤åÜ:“3ÔNLh§6Î›Û¼GeBìõòåèv>^Ë;îâ	;é·ıƒÿöª¬y…1_Y¢¿4rŸùc|BéR	zÈ¥³‘¦Fs"ıŸ ‘pÄ	2C¯«`³%„¯Sñ«g´¦*¦H²)ì”hL>İ?MšŞ]û=ù–kó¶o“b„&ê¼Ñå´ ;ÏÅŠ­äyxÃs˜TãçW÷ªÕ<ëaµİßóKïğÛÈÒrÊäwÒŞúôâZÒ¢ĞµÿÏñøÂã÷ÜªÙú¥hZ\9KéGPñä¥âNS¥Ìz”oD”HW¦™EK¹ñÊ„Ş Oğ’åŠ£WÌ$sñK{Å.·Ì8—[$£\Å:ƒ…ÊûròWßz9Z^13Á¡è^Y.XZÉ¶{XšÈ\¡"”DÈÑ—ä†À¥«’°¨”¯ŞC+É€¤£V@Ğ×Üí«»Ef·áu •¶º5Ç»×C¦ï@Ï’˜³¼œ/—Æhó¢hˆ’ÿÎ	QÔCjÍ¯5?·û§§ù[_¦nUå˜AhdNÖHê×YÖ ªÃ#a…>&ú¡+öûQ7-{³™óåj}*âi|8Ø¥óæíSg¶E·J}Ò¨
tq._ø¢*7N­~N¹wvŸƒFø­°¶?eËktUtŒo„bq‘½˜­GZù´ò=ò•W§Z§iVÓï¹uš –:Ö ßÄl«5f‹ñ|4ŸQ¯«5®Eök	Ü•¾>z!í¹vìÓSåè[,fãéˆÀRŸ¼~%ŸÉ6%¶É"ĞùÕæANVâuR%Z¡”ÈçæGµ“…ró¿–—ª}ò_ùªÈkf“õˆŞ	IkV›øäÛ>õÈBò—¿.-wëowLwúwÓ¿s<ÄıRÇtv4Ê7™í)"á›oåu«a§ò‚7È-Ş3¶â()º1mƒsá•ærjaèÖq-œ.Ç¸ï×¿‘14`ÛñÌÄËù8şáß‚pó7€/œ‘äÈ%JkJ¦ãÛd`ı¸÷Ğåc›Ü˜Érp´Ì«ur£²–ÂïHq»¢Ø—º´|Å­ûö§ÒNş9
ˆüIG^Ï*$àğTçÌï’â0æpSxa$TîÙÄ³TÆ|úëúöx‰#Zx¡d·‹\ÖüXç	$dÓ.€°WªÀ£ ÆÉS"ª"ãñ—	IØòúxŠ$´»ø®ì~uÃ;Hªí•¨Psf®ÑğñU(ü:ƒ˜ÓA$ô¼ß‘¨ÖN‰2HÌ§·¯ œËú!WáTäÅ±ôé^—"fë[Ÿ™dÊ-xgÆzjñÓ‚Æ7VÏOÌ*`;]™Õœ3Ãhåb¡m İnó¨’6œ9Ïpëmüº£7ŞMÚ§F÷É1ÇÏñ«79³B¿
5I=k˜3<ŸKe´*‡	r?s¯wÆ"
ÂÚ—õ¢–Ñûà]û<8o†½y»û¸±{ŸÈ
İ/¤zvï³_†P<5—†šÓ-Ra7ÁG›åMîÀpâEãdtÓ<>­@\'¹ö ÷X…p›‡ç¨G¾â¥”¨c¨½YN)“(»Ğ}™5”?|~±ƒä©[Z]0£m¶í
µ«Áè€şÚj¶¼J¶ìÙÌì]XÛåhØ²S¦iN-ö
r-º•‹naÇ„èîN¤ÊÉÆbQ¹=2ÌM!@ü·Gvd?]Ëİ‘.<Ö`ÈJÑÔt›Vñ1| VcŸĞÏ¿ù®GÖ1ôıFÁæ.cÙ[×[Ùîm6şWºø|K#±{Š%£€˜Ç¾©‚Ò:jfÕ9õ ÇÁÕ©Øn¦úK2œÎ!õ©F`4È’[CİØs…S"Z‰ª“!K»çR@¢Ç9 A+¤Yàd‡CÅ™-	Q²´Ûø£5išèÆDÕÁšğÂrt\¿Ë ¸Ç,FTHñ«Zƒ3A±GhÙ7Å€†˜è/GÖPÓZ5º!ı­ÉÒ|f?‰0³ØÔ4kVåQbHe†ƒI@êjIÿ)†4qxŒ	Óv…B¦ãÉr:q²^óS¯GQQIÁê·Ó”À›r¡Ç&-^º¢±Õôòç°ù	¤
Ãëåbâ,†„J5#ÛàoïW¿~|ıaõæ©ùŸÆÛßoö¡¿m`ş|qQ&˜Ba;¿å¥ÀLkòÒdFãMSñæãÛâšMôqq˜³àxÃ$HÑ†Í«(ËµO/Ü¼¤çöñé}a+åóˆcúBg”²"”ÍWAŸ5yÅŸ	Ä1¢y4wš€E^ï¥˜EŞÁ¼LØ"§áÈEî®$x±È§‚×{årÂæ¤mÕÙ²ÛÜb¬õö5ìKEVCcZÖ@%\£ÀÔg„·¿¼CáÃM@lûYÔÒ÷ƒÉèfä´²3Ú"@4‘”5÷¨H¾uÊÏšö¨ ½ˆë@)ÿ—ØúÏƒxcÿL¿#²1ù¿?ş=UÅV+ äCØêºM]Vás…¬Ìº"æ–ô¯9Ö)ó)vÊdlX;^ş-Ê‚¢—ËªQËR8N—¼€Åd*kŒg3[8cÀ·/öuBW 8•x±HÇ¶Õi'FZôq¶yÕven:NÅÉÍÈ]¾”xßF]½$—it	wäÌ,+^Ì%—};Í»@Ò UV*7Îòunšn§{E•T}¦–D]—a	dyrŸLÆk´ÄöI·´ÑxO8ï­4–4cx+»ƒø‰+¼vÒw‘•
!™,[§Ê;}n)wä`OMO4UånË€âR±y˜Ş-d[³“3ÇS£ß‘Ù]e­)¾(ŞVÄ¯G#kj¢q_ëñOeÓ¯Å¤•OI,R o¢   ÿÿ j1•Î©Afs÷˜s®ĞwU’­— :‘Æ¿åE•Š¶É×ºçnCí¹ÅÃõ–m§
ÇAó@hJy“¹ÛØüØ“ïf“–jéÓ¬‰Ö\¹Z3{¢W^Î-×ë	v¦Óş ú¾äêÁµm,ü*}`W4S«ï‘Y‡ÜòÕ Èê‹¾ªßñ¥¿Ş?‚ZX¸•”¼í¹p*šDEú*¥8¹(ÆŒğ	BÑW¯“S³Ã
V>—ÈD…TT†ô^¿ßNŒWş#rà›õtç¹_³MæÃÁİ}€ [¹Ù³ØTs9D0×uƒ¼UÒ·÷úo)ßµ6”|_íi.7¤(ñëñl1şëÛ•²ñ´ER‚ı$ÉĞêbf²áƒ­ÔÆåÄ nxÍ}Ê2®ç=p8Qñœ‘¦k1dAÖï¢€š×Í³>¼Ã–OãX„U¡H'ğ@Ê¿œâ3ÈØˆ¹™Sc^PÎÛ¼ë{’×ı–Ğ]œby½pfÉ^½ê„ª4Í«gjä•şÚÄé7ÂiâÂo_”±úØ¢Em¯]½SÁ¹Ã»øRHG^	úKU`KÓ0¤S»ÈêsAQ«§d&å4LîL$µä/AW™¼¢¶_]µÙ0ÃµæÃ"ä¨h%)@‰šKîÍğÚşÒ‹}9›ËÔ<"Sİ2°D¦úb˜0ì·ñõ®k2éÛ&3á9 Ä{z^‹,•@‡B\ÁNª„4: j·ª!ˆv/C
TêzÀy©•
òæg*_¹VW¼·°ªæL‰(ou‘OåsŠ±±ìâœâÕËÙr>›ô9…öKh¿„öKh¿„öKh¿Dô»N¿„vh7vh7tOÚM İOÚMĞÚ€‡ømmÀ;ã©Mi^ğİğÚ?2HmˆkC<ÿiC\¾÷qmój›÷Œ6¯6<µá©Ïk3<[›ƒFæàÔ4L¼@úŞ™6ÍAmÖ¤6ë´Y—ÿzkÖi#G9mŒm¥h+åiZ)­3Htr°c/œ©3B¦FòOÉkH~d’·‡ä/>u¼¨ßÓD|­ñÎ¬¼³˜ÍÌùDÃ0ëB°‚V G¨Ö0G5Œ|RVäÚIn®ı]àZù#n­ÓoO×‹ÅÌ\°ÙŒ¤óïYV{À-ÀdLë»â‘LØ–®’sÊÑ„«íYR”'ºµe›ÆÔX^4Ñõœº¥[ß#6ö,|‹6e"gì8xì˜¨I:è­»ß×äÛ£Ùh¿ğRİ‘9ğrºÙ‡ ÑÙ0ó—éáÏó>à`•=´íñWìíÁñ²Ê˜¦Ãd­%êL‡ƒ/d!|E²<ÙOA¬ ôÊ7}+¡„F(~?Œû„KL"¸³§Š*&!Aã(¦'µR8"NÙêSs	QÈ€{9ŠIéC½Ó&PàÔ¼)HÅ”ğ…-é'ßöoıínƒéÿ©{+À²Lkf- hKX²ÚøJc+@Dv2/Üf¸Ş‘&ú&Ç«²„”/¼¯–LE¢NaÎ‰cŠ»ÀwqÀ\.öÚµÑˆ^ìv„è?¡ıg…`b{m-€Ê9iÒ¾ß„xû|°!ÿ{™”›íÂ9i/YÂ™š.·à[­’„T4¨>¤g†ÊŞñ³Çp,ÆÚğëXdƒEe>ÓKMú%£nM»Ôcü«çş~PçM2_·†±0ù¹±eBIÍ^Ÿ^YRv²nî½WÓPãk–}(Ë"  M»H2|ÁŒïÑ. o…ÿ¹óƒp…8ìœ.k“íG­å&o/Í`ÿbq@€÷hƒWhË}˜Î&.0Ÿ´i	°Ï›±ø»•¸÷”XW6‘»Ùs¦UHFÆÏ„O;öê³6ó~¿,µú8–ª©v<V¦j¯Éœ|	’©î†»WG”r²hx|ÎØ¼ŒİIéBôRõñÃÄ^%DƒÓ’ĞQoÃ.e1¦>ÜgŞË¨tó²‚Òë¦5””B•ÌÍqúe‡³€VÔ“zÊ7àG—K*¯ƒ^9î.¾íàR]SÔ»L&É34ô<¶ Á5é¦ofnHÌ‡™p‡ó‡ÕU6‡±–¯¹VD|†Ñêûd˜"™jÓaÉT$±°L³i<M
—òï¹ÓÂÁà	`Ë"œê4D¥Œ¡ÔÃ$µ7|ìsÆkì(Fí*ˆ&$¯€•øî,C$*A»[50D%Ò€ç˜bàÅ	ÒÅ¾$¾g¼8Í›xå}€xnMŒÙxÜÿ ^í9³#D{5J”¥½Ú«¡½Ú«qî®Ñ_ w.hZ¢+m@kZĞªÚãÉõµË™9^Ì¦mÔ>E£VÛ©%bÑvª¶Sµz‰vª6µÉ(Z­ÎdÔæŸ6ÿ´ùwİæ_c£ìîXd´ø}±•]-0§csñjÊ¿Z€,ªşjâõmlm\( _0ÖhßÑLQ:"\²¿Q´¸ Õx2Eœİ´§k+¥£ÓU–SÒéÒw®G¢í-ÌvŸïY¡èÉ/wGi¦#hù§;"höŸÂöîH¾É¢ë«F¾J—\™Lœ¼xõÂœ¿äËÄxªH¼ëiÉ"õG6]†ÀI–MPŞ\‹ ‘Ï¢ó€¼{|ëo·Ï•1ÿìÕxôòÅk^æ.+í0ºš×/5Ïr|/…™\¦ÛCL”È­
ò‹ëÔñQ‹1íî¨e2uÌùØÑG-=<jÑ!¥­Ô!ú D„è¸5}ï>èJ
èCÑCE(~ÔŠã	Âó‘¸ŸH£ø4Š×p¼´'_#ïhS5¬Ö°ZVkT{E¨öÿ  ÿÿì]İ“Û8rÏ_¡ÜCjSå¸ô-qïn®fmïÆW»·>÷î!•R$8ÃµDj)Ê>§rùÛ€_ ñA 9”,=Œg, ¿n4ºİcÑjÙAhawÓfM´Ùé¼?mx~à¹ÛÙM›½i³7mö¦ÍŞ”­›²uS¶EÙ²¢,¨”ï67}­§›ë87eá¦,\‚²pÛËn{Ùóìe*²Ü{Q÷§İùèƒ¾9Ä¿†ˆÙÓó‰’à?»¿BÏ¤ q0õp8AtD¿ÊŸä“ø}³Qm6øwüÂèÑ_Îš’Wïáéˆf¥û@ã=6†¦ç$šüB((Ê^[ë’ù¨¸Ÿ&9òeQåz…ÔÂp5àù´¼‡8!“
³Óù
lf›å\§d"ÀcÄe®~vh¶©x^M§&À%k?>îáßBøùû89îOE9"Ğu6‹­ëÎVj–WG¤„QÜ=ôÇ‰a{²õ9%©Æ1‘îı;D¤oòåÑ‘ŸØ®ó*ŞÇ‰ĞÄ‚.[3Ôó	„w÷ñ1‰‘Š¶Ë6 Äòˆùı¢·¶Áê§Ş”ğ}¬(İñgÆ-fˆòßÎÉÇnä¡¢SÛVPÇ­]©FIàé¼'ëÎû}OŸÃÔ{šÔ0Ê=e ^Ç¢Ï—Ôß2Íñ§~®=úÇ"?.zÑìWÌĞ¯*Àt˜Õ‹á„B¼Zï¬ÌJØ•ÆÔşÉ“u|½«V‚ù<^L4föS¶ÁñR!Q#sWKw¼`üfÂôB£ÎäO²&ßNşc¦¦²Ì¸*GÜv6KŒä¸Ñ‡†ÇÏŒ¬è–U<s–¨“"ÉevXÎñ¦ë….‚å(İAğ5BçÁŒF«ÚÂ…_„ú­J^-î`Ÿ²Äw|ÕËe¬è¨ã¼µyÜ­­Ûå;mÆòZÙxi-Kn­){
¬­rÿ”Ér¡hG o!Ø®Áğ®13ù<z¾•qdëÎı÷Ç]¹š0U7÷°öy~dG³w$}©¾õÏLL†Ç¸z?ï’8÷ĞÂŒªìâ˜eéõ0ÇP¹D+Ÿı|æ€Ç$´Ôää®¾¿Ãä^®™ª(ãnZ¶>f*>°
Ízã¯gĞU‚ÎìašÂBí½¸;8¡É‘ö10º¨ã¹~(zìÜEƒ6×ÎXuÊk<X’Zãš¶xùgBtş)àCo]N•v±Ç¡€sV™Rìà¹X‘#0ü³k}Ş˜¹çF»=üÄ‰WÀ­ğ	ëH·LDó>5½l™D#—j ­(áîôŸ"ªâä[¾Š”=ßT¸Zplcİg\–,i]úfH`rÉŸøçŞR³Úæà1ê‚$İ5SÑ¦)Ol
‡¡Bòæ*/7rKÙØ
 kû2á»1ß­ø·;œrXcÙı…6¢Ñp6wœÔ%…F¬MóÁáL/§Ş/§¿ÄéO±!ô»Ú;è¾¬˜bNgÔ§áå4 Ù…&­à}2±¾¸`¬4’ÊvÑFzN3X‹+l¡?åX¿p[$-Ÿ$FB<*3~KNÎRƒ¼÷·ßÖ˜õRLl[L³–£½ëÜ#LMö@1’}°Ëy Ãr;ñ‡ m
õ«	ÒoE–.Vr;®=È ×€pÒ$ß s¡!×¸^£YÅb‹‹¥8GxÓØĞYGØ&ŠIrÄò6CIkáÇ0â•#ÁKJ®Òríwi|,Æà~O&Fnü4İğLe«øœ¶4ËßÛÃÙ÷dĞryö$hú— Í¯L|ü‡ĞîH/]ŸGz{ÇªWıSõ°áÁåG1¹Äa:…Fv.5âôxháq?ı+şvã¹koü‘ˆ™LQ¼tİS~JD©N}|›PËEáŒÎF&?,	lq5Ù¢ÁÛ–Xc½\N7lÆÏÔ&.àf©`á–.x¾Y8x<ü…?Œf ıpMa0 ½—gV×}®¡^ôçn"ª	êÖWúñàŒCà)K<\ÃĞF?søâÍy._ˆp4‚—ß–a5µ'jüÕúÍTªœÔYz¸›\®öCµ1iôù÷y”àhïPQöOíÔN	n7¦^_{\àÌÎ¼H_6'×ÉÃÔÖ£İsO½˜¸ïW²ß&^†€óùL*IMànâçCé…9æûö]9oÍçqŠ;0‘ê>ùË	GWÑÏqø=*úIÅ•œøCQVÒs¾Ê½$—t‰×F¹¿b!$ıá»P—Ğ“¯ºjób-•=x•T“%°!ŒÈ~`]"-—.p|ßá›şù¶Ì›¬êé*ƒæÉî&Ü®X¸ñÚ²ç•¯Kò‚ol	Bo:Ö„€ä‚pÌá!¬…ìı&6g³S±$bb½ğ%œÓÉÍ?]…«[wùm/ü:³ÀsÊÜ!¨»?ûp'paãâ 8Át‡ó¢É¾çxß—Ğ‹6\gxÕê|ç^è2ß‡‡ÕãHB¯ñFw\1ğñ§%¢¾/gq™Ê
sµš¹XÏJjÏNŠâ Ö(¹©Ñ’â sÓ7a y«õÒ$®‹oÿ’;‚V2O:[àN§ZâMM¦P­D"G,ÜTD#ğı]ÖFCáo$¡<øÊ‚(­áHÁ"'éW#û–bü¬6¥X7A#’bİdcÅN6$j¥è ËX®W¹™åî¥TtF°ëmg®ë]‹è„D>ä“¬dç…ÎgûõÈ™ş™´À8ñaÒÏYwënfëÕvcë N<v»¦³kÔğ}7!ïaj[½¡¾WÔ“Å4½ĞÎG–»«şû>Ïø ì ~±^làtÅÉN£¤óÔÛKÑz^¸{Ñül\;j4Y—æâÙà…õtîl|èéğB1…I8Å´@t
C¤aºWºÀzìC`¿X®®µ•ÚsfëÿYó˜·Æ^ğ3	DàÃTƒV4oépÅ¹ä¦`Í8œ‰í`ŒèF”‹kÔº¨w;Kg‹¦)†İó)ŒŒ=`-Å–·òf ĞRº€ñ»ıãİÿ&¼èpŸ 	ËòX1/5H+fÔ2¨°‘§ŠÓ¶–Êª™»ªÙX5o;H÷œU#½ê¡'ÎKŒ¡!
àõ—·)lıuò]ä2äRêg¹İuĞœ–95`àÿÄ‘•=7œùÖ	8µ¯rÙÅ±ÇçßDàÀwœS¯tˆ%^ùƒHºr4‰„+ÚH%[ÑHU¢U^Š$k^›’ğ=ş`h=PÁã:gò×•ZÑó6˜Ví×`JúèÈ’i¡ Û	›ñxâAl¤ z7)ûQ
Øä¤ ¨Ï3·Ş3‚Õ‡³œ¬Öy÷¼`¢îº¤«õic^=$kÌ±ïa"2×“TÕcàÓßL7Ó­§~4\b…2w³„şf6x”&-Îäé²Š–Ê!Î:i»Æ·	%ÅZw«™’>´íVº [:îb:_Nù§øñú8ÂğjDİCHş×sè}|û/Oq’zgÀˆ:èSiO	ù#Ôd;êRé“íí­n$ìÊ®*"Xª^õÁ«•˜ÁG_&Î—+gí/9Zócü)ï{ğ]¹øJ¬LùÄ‹·²ˆ·~˜Íœ©póWt¦ô‚Ì-ãÆDÖ–z-MTüV.·ç²³™Mç‚8‹Úº7¾g$çù¶œÍûYŒ=z×A,‚Ôx›`ÕJj¬š©Úé/Å2hÀŸôcZVBÃRWw!/<šmÅ›¿WDAÚWË(ã!4IS{|ä8µ{HC÷ß!ÃİM×ü3t_vòÏzì)ÿL¤CäŸ¼{ß¹†&ıíz83èÖâ"G ¥ŒIe>Şzë¯×ÇuYójTíİ„ü¾Kò!oi0ÌÔ€&å&ÕŠş·ì”İBh®­şåg¯x„Õ¡ZKM‘d+XnÁpU\J\zmS½VStÇ ğümğ¬§‹ÅÌ™r"9Ero×wN²*².ÚxOĞûØÅá!	÷šùÓÀõV¬Á7 Åz\%öÃN®2ÉiÄ›N-Ò²Şuyf$cêK‘ÁJ¾Œ%X:+#¾Äaúmğäl«Ä¼Ù~B»mîúòR°mÕêÖŠ¹@/\¶õ3]÷yñ„8ªV[{›ÕÔ‡/¡Âe¬~ø­úöGÄyw“D~|Ø‰#OşBn^'v»ÿ%æG-k6Æğ([—(1å1îa
ûá28›® 3D¬Yb£#ŒLåRw'œÚÔş7]nD‘•š1#7´*È¤‘¨)Kó}¸Oab+ÅGàÏ}©î,|ÊŒ€»‡#…Ïõñ›}“Ó^6áé„‹o£O¨+w×ó­çÌçK£„^	<„çÃîs|Şû;°ßÇŸw!™Zë#	üí&Ğßq²;øÇ<ìc¨‹7&€©AA‚˜cJ™ê-¥ÁLõ¦ªMÍ.%¨I¿|¥j‚’{SAà	qÜ¼ ,t’¼ùÓ–ìMH^~f/DT¶aI@¤ÎWÁt«Í^Èÿ…¸-ˆÎM°òa Ö¬|ÊUŸI–³ˆ µX‰:ßMò¿óéŠB$"‘û¾ÃHFîĞ2É{@.'yO(‹Kşp—"5s8!JÊuk‰T•Ä°°“Ç˜Ôq{¡9IU­)âX‹õ4e”÷¢î±ƒõzéÖ¬c$A­Zòtf«çD@`ù*›Ä$ŸŒP‡+æ8Ö“ÃÕ<ó11ù‘³Fµ`š°Dt~øù/VvK³ÜúSÎA#÷UâÁdûZ9™A¶²r´r÷¢şóec'“mXÅ3ª{T5F÷m©MÏÏäöØ³»óiâùk²8§O8ø.NÒŸ29ÿ*ö­œ¾=×A*dÀQ!s¦8>ÅÜEçƒËñúÔÚxhJ»Ì1Êovˆ8./Ö®38Ñ¤Úº\ªöĞ¢é€õ»$¾Né{ø	FgøØƒÈãkúöÏaúä'à3ØïJÓa£)±Ûxç„Tp³ÑùÀ'îq'òf1ÚÑy±y!á!|ó>wá ö®±¸‚!ôïŠˆz2¼Ì"ên·_`2Û®íÍ°ìÒîuÑêõz£Eô%‡
1e°»,‚`3ƒœ°Öç5æ±rDÑˆ×,ªæ8üaÄÎÃ!d`£Ñ]KvºÆ2µ¶gÖ¦õ‰Æ‚XâÁRÎPÛ¥˜aõùq»\lƒ™ÇÑöìÖÇ¼,.Á®D@¤ƒãa¦Ëƒy9Ş› À*ŸÒÂ[ÀùÆ¬üOÃ7À×÷Ä±Ìğ£Éq2í“ç²©ïpa93Hè°;¡†sÏ¢¤ˆÂÉ‡Üâ‚l’yóDsPÚJÑøNÇXÑxTÕfÁŒ8NÓ…±×±€¬@M™7ãZ•…øSàZ+Sƒn]š9t&WW£ÒéJ¯l&‡	ÍæıÜû†L	›³SK*˜Êƒx¡Õº6Í¸la@$ìŸ†•9ÙOá[ºïëòOÙÜÂ5×ÕœÃÉz—ó…îmö×±w> ]ånâç¿é¹•Åo:HŠfÔÒ„4(m)/‡¤×®IDš][œeOiCšË8LêÉ°é1¤éëÈşúà•V³¦Ì<çÖ‰ö½Üõ}w=[s%VµG¬ÃøSIjyEƒFÊ©0hI.¡ıºc8d³‰kñG„‘S\µyI@MtÀK_áëâ6NÔîÖqÏ\ñ•Ÿ¨#ıìâºèÌLÒˆ%_˜¯qÖm4ù¿‡éÓ›(SœGÿÿ@,­3P#È gßj8úÔ[ş¯Îy·|Hõ¤Kr%gÜ
DŠFx=<G Öá0³…mµ¿
L\	™¶ıjU”æÇ¬|ióe,ì3ÖÅÉ¾·)ÀW[Â'!šFıÄÆWbéÌ¶¼Üàº¼:Ë”NÔ´RÀ¹NhŸşÌÎrÊ‰ı1)ğyÃİ(p§‚¥GhWĞ¹ç{+È9fµ ‰sk:{!5ÔğXËO~|­µ5m²qÖºPÊnL!ıœr7úúîlç+tájéRëwh+æıòÿ¼?\×¬‹=?PdM‹_²ÖË,·SóİB·¬[^¯SLÅ­Ó"`OŠÕq³Ìƒœ9ãÒDök“"û´ê‘‘7î•œ-rñ kó¤Çc®¾Çd9¯ïõÎm,Wë,şe¸XíÅ¹50«º‹2ÀS}°ÕÇ:yHÑ«³ 0w"¸ÁbceÄ ¥„Yóéf±áDÔ‰÷şŒSÂƒéé‡Ÿ&9wø/¥'p1‡§øœœ¤Ï4÷£ù‡iìÇè_89Ÿü]Rq»œƒl²’ñéÍVÜLgÓ÷¢ºùÊæq%›°^¼F·åTCw&Ö£õ?xÆoBıÏâHïÖWî5aìK4 7qò¯Y¬…Uk²BÍlÅ5œ©^*Ì4ù: Ğ<s…ÔßË-ø—doc¯œùÀİN]ÎmÜYxNöÚ;O}–Ï·ÕçÑºÕšwŞ‘j½uÚ™óç…prÉ€Ñômğç½º÷ˆ›Ë.œ%ÜsqÑPş¬Ïòùø³>Vş¬5ïÌŸµŞ:ñgc^7ş>Âô¡c¦ïÌ‚ øZÇÌb™|°á9Å—)ç:KÓ›òüşù9êB=r-ú/^6'fXcfO*Ú±ªK¦'XW&çËÅÂ÷œ¹	G¨€¾ñu–1à•³å=JÜnï>Ç‰ÿğşİä˜ÿ>fş©k{×ÉG}sşt½’Kz,àÔ7£Z×*ı™¿§Ÿ4^>©k]Ê'=ˆùWO y´’‡‚éÜãŞëÍÏâk¹Ç$şú0Á9ìĞlZ.ËÖg>äy$’sôÈ¾18edj(ŠÑ.%édóŞ¥ä0A¦ÀÀÁä,2’Ä{œÓ‰ÊÅJ–¦÷g	<‚úo£}Á<¤• ™`µÙL·^%ªÖog˜|á…¦~§Ù\Şg|”áWò…[ÿ÷‡÷š_ğÈÂ]Š~U»ÇH^ xOp—†6O”IÑ^F0µ|Øô¹ü9Ğ¢KŞTG–É{ÒÎ¬+˜Ï¥H»ø:wÍËui0‡M÷Rc$·½8ì¦^«¹d1÷ãuWdfB¸ èœG¸ZçÖSSµ\ªw­Ùü®¯İh»Ú@×w9Å›	3cü Öe·J9mÎ³œgZäâÕ¤(F«câ7å)Uêi¾GGB¤P’TS\–A®qœ ç¦M3B}ì>ÕÓ„=C•ä ³ô{45„Zßü{µË.òÉç¥™eAoŠµdj³ÅçÿáG¤bœà›âù—~üá)‰?ÿœüWµï~ÇÇÈïøšó²l‚½ø;>eØ{øu<ˆğ¹çÀ¹Ã)VF]!n|ÃËOr*¦`XUï’b±(cx‹?:—Vñ‡"pYF²©Ù˜;e:*Be³õëvq„Z[±`¦Š›H$Ú—ëélê­ËŸ6@pcnAäTéexO•ï'˜>ÅşøP¾3qp:îÌ[¯9)Úœğÿ²_ç§•ßD_\V›`Öòn²Ñ#I#¯l?+ôZÎWûğªeş‚W B8'­=×]¯MàzÎW-^ÖqF<Î9ıÖŞD;°¹‰å­Â—°m)Wh°Ít[M6i"òj9ª^Ã}ˆ`ğÅJ¾°ö7‡ãm,Ç3G‹Ö¿}}I5«¯«lÁÆx!'ÊÿüßÉlòmşû¿MşoÖw¸„JØøÀğÂİäCíhşa“ÿ11Öí²^Rj¢ìŸ#&{è÷ÿR½Ñ…¥ y¬éq£èlïß½ziê‚ê¿J£¦‰¼B›é@×JDz@t‡&½i÷!KøLfsÉ˜ps3]×ìğU¶lw"Uìu¡6	«n™
á}1[¥³şÔNªeWãÅ£„×–N´Š¥SQk”(‘6ªÚ­[²¨¦¡¡ê~Iáı÷$‘x(bM7?Fç¯³£ µ¾b(€}ŞLğu†IdR²–Ã„şF+}	ı ræ’úh—Ã¡m,ÁVè;E¢¼‰ƒİğúü‘@rˆ,³…w­P½*Ê¢n0á¸— ~Ûl&JJõÊŞnq-L-,®7g¤Ì¼h5Rà¥§İ-o4.n‰:èSX‹Æ´W™ ¡&†OoÇô‹~®Yo†•]Z››İ*‚ëµŒè%¼EHÒ×» XÌİ¥;SÖä«ÕÜéb¾İrÊ×ê®Ôæ‘,ëÖ\yõÒ;\»]»Z‹ºi…ªx{yƒÉÔƒNsúHúÆ„TîïºÃQ¥q£Ê…Íû<Q–ìŠo·nê’D´koë-œ…ğ²Òoãteˆv «ñn´¸äàŸRô<(‚ûÓ.÷ğşœÆ)Ò­O{R±ËJ*œõ&ğ¦€“j#¿LL&0É'"öó)cÇˆ6{	³G‚®|5LÂŒ±ñ6÷£’(+ÀˆDYîF€|Š­Ê ç`
×bC™q×cÇ›íƒe×z‘îçÇGxJÑFŸ¬Ôï…[¸Ø@£Øàáâ$È.$OJ5ƒö1	=ö*ÆÉ{‚şyw¾,f4ı›eëã•'Ø†	¶ÖªfŸ­dkÍ”¬±¯Ö[ LGzÆĞ*Ãœ¤0¯’ÑkÁ5zŒµYÍ0'0ÒÜrÉÑ©|ÜğSí(Ñ’›¿(Á¢ñÔâEİô—]Qg–õ²k¼nLv"v3¾&¾îø‚<°’Şd;›n–«§ºMû–áùÚ¼ng÷j´È¨ÙÈè˜|Àw,Fª¸]ÃùAG¨d‹b"RŠ…¤¤„¢˜ [õPšjy i®êMªS²ƒì‰ıøm
Vâ\à€Õ<Ç1¨pÓ½$³{1íŠ¡DJlÑ@¬¿-Ô	Êî.Ekå¤(Rdaû-U9a*oËÖÅÃtÑÉx…Jù;@lğõÒq9˜KÌ.íÊCœ>Áä´ó@´Ç#š‘JK$İ¸ÀŸÀh´¤¼T9_İMö˜.Zùn
b#mÈP2iƒÈ¥n¡,m²î.EÚhŸ‘è)]â Qñ˜=€ˆÃŸÇ27cùußµÜâbVãñÙŞúE7)nüúñbjÙÚÑÒ¾Ê¦-ğò(+_8˜à
2ø;—­—š3êSV6†²D”Ö:ÖÍ©?o(Tïd·_`2Ÿ­»Î¦ìÈn Ğ‡ÉzŒJ˜¯ÿ·¾‚3ŸÏ\w‡é×Óœiö$»KÁ¸67»èøv=›¦¢¹ıÀ×/[#àFJt7n ª{Şl^/ZsH.ÒÄ"(cÿgS>ÕVÇ×h&¼zH7š R¼Fsˆ3Uà%NE‡H›ıß[ÿšÿnŠtüS†ö†E›’ÑT*•.‰ºpº˜Í—-± F†iÎğ’fš±‰Õ%ò¨¥‹vÜİŒÚ™©£±ìzx¹ä:WÊ#ŞSZN¤ì4dÓ4ŸõlºâC³ÁeZ¦™{\4{xH.#ì=}¿c+›ÚCÜ•‹³Oê[6ó·§]@~„N§`¿óA¸çŸªx aÄO²ù„é.÷lI8Dâ,vE¥Áû3” ü§9¤0¨ÑPÔh©ÄtµfÎMœ2µ.€f€µ‘A}®D½yGš3TãÈ-ĞÚšw¾š™µ¢0ÑÚ6B§µf{-W¿e›‡zÉMÃ–=\«­f>_­7³ÙÊd«yÈïiàÕ§&ÄÈÙ«Q‰ÔØ¨ó=d²¶æaï’8Ñ)¸êX¥éS! †±g§?–Ìé›ÆZê«^~ƒnÓ tÆOĞA—i:Oã{ôO'jºSã|
½NÔÀtÆßbÅü\¢i:OãÇ0úØiQH68¥+£X¾ê^I¨^½W‘S
jJ@ê«®³õüµCÅæŒ`ßÔ£‘|v¨±ô—®¿¤ÏóG"€íPc3÷ÖKgµº`j}À5€»X8ŞtzÁÔ Û‘j8Á|ãÏ×ğ‚©AvE;Ô€ËÍfêÌ/™ds¶C±^!Áá\05°’`IlÌ½i°v.Lˆ>€OĞ¯‹‹éz«’3~ş›^Â^jF}6©aì6Oe§šõ—ê:,·C}ºê«Ë<ìêÑÔœzÔ£¹d,aNÀ`Xl7[ FÛõŒdÍ,¬zkg5K0æÎğÁPÚ™zi‘ú5U™ÒÊÕ[É 	'V’n‰ºØÚl·ş&€’œ½U)NÁÕ×~KÌ<ƒ ÿWcı}ópˆç®É<Â^;4‰Ÿ@™¸$eY|7[—S¿'î8¡z]UR™uîª9B{AÍbµ]kšHì­‘'pÒE¢ˆ¥ôB»Šµ]Û¯7‹9pÄ»v{¼×9ÂSb¾&`*4Ş–_o ‚”Ä±ß@üìA4Ù‚š¹ı•ÕÍtñ9:ôÓÚ´²Îûá	4
ÑWôi¹ášÊ¡‹BÄ‡WU¿µU7ç;&á'|A„yÙæPWêX/Ê«ŞŒ²ß°ô(r~Íy&¦Ï_¢ğ·3Äµzï‚>«æÍŞ¹VŸº=íŸ"‹_EÉ8x“×?üæ¡^»“¹ØIÍj’’Ÿl%i÷²è=ÀV¥fOZëíEÃ3Õ»éoBŞÂoÊI©ıĞ,l:6IJ·}V¶<V–ZzÊäò .«¡µèwXYşÅrµoË.-oïıØäÉ`š¢÷<İeÈÆŠ;ù{|èÁoÓù|ı%ŸuøušÙ‰Èµx·ş¶-Éh4(šsÂ	ù 8–éªç×êã¦<4^»OW‘¡cëœÔÈCöPî3b…¢Ù\ESi>£Ş»ŠÎ"~ÆHq¡“»	péU2Ac±õw‚íb¾Ş.·‹qIêAäh‡E°GÏYøĞá%Ã¼)b£QÄÄ,o‘7påP¬‘ŸöçÇ‹ ès ¯½øø§\‡ŞÇw ôq%+Î‹5\9H®÷ñ)à.:\qİÌ¬ûp—ùäÍˆ·.ˆ“Ãh­| ‘s÷ £ôz‰ë1÷qØ‚^OÕÖåÊ¶úæòEÖ<_ÉåñWY‘›¿Ä©†v-©Ô²p‚õ"p‘;š—O1Šk¹¾F„Ûë²N«[yñŠØr¯2U»S–gü»0IŸ|ğÅoµX,6s}æ´²;·˜PñË8˜WÏ¾¶,êˆ,–Ç*Ë&H‘ü5ü zE/Ö±<HÂšÁøëåFìüË‹iŸ §*K‘ná#Ü¥qT¦\àµ¢¿ÌRú®ƒÍ/Ì<+yH’Vò”jamÉ°—’ÎcJ·\vm:9ÏôŸŸ¢ÛVÔÂ”lUÉš¤k}€ÛTé€ò¦Àä[\Ø[YNÕÜ@Z~0ûhı¥ûu°–²â—|áW³“yCyíeÎÖ²ıëĞË©Ø0
ú/Ú÷b²ä¿G	Ëúœœ¸Fúİj\Æ-²ä·F“å®ÖÓ¹$¸5³Ç#]bE8&ğÓˆ4æ{2¡s’@*…Éõ‘iÂIFx$;šR*C<Àq¾œv{(ÕŞyî ò¶47ù)¿¤yëŞP‘¿= ½¶->M:ë_¶"Ö²Šd³©ÖM/4°"¤×ª\'e»"™z¹f:ZS}ùtfYrë ¥—Y7=¥«u5­`G-}ÛQì¦"ç'˜>Åşj_ß™ØÜÜz€w2‚²>'Î‘ÿC?jT]qºŠ3¿NçÃ$xF,8–DD ×uıÅÒ¨äŒBFR*ß‰Óx·Ñ£øëHÑ(ÊT‡Ã¨zİ‘C*¿­ıêp+ÃåûT+©Êß¹(Fg«ĞÇ?eœNWğşşŒğ_¡Ğ:Üã ä!øY¶Çf]#>ŸNÙªBÚÁHÌÃ±ÚW[¯[;<ôCâ¤GJ$jA”¾ÑG–2{º!œ‚ÅÜŠ·–Íúºš¦h3HĞô• ™½Ğ¨¼6—„8§VÎdÈe)^\M‰ªü”áı=RÇ“ĞÊ½¥åtî.— ½ ¿:1|
ágQíİ4D'«0Ú‘6~şià¥á'¸SiúÃÇ§t‡v:Üê'é.Aïï09„{Nq>üÔ	Âµ‹!ÓZ@&1¥qœÆ–xMøOH]&üGTı%¢ûv–<£ÈÍ=Ïšrrvhí®÷Öö|6i}¬•eZ{²Ïs:»²ÅĞ±Mäë¢åáªÓZg4şjéôĞºp:	×P=!µÌmzXñt+±pb50`c¢~-§ë·‚kí*!Ô•‘ôº›²éÏ­£œïµäo§É¶x¥Ô+»E^iƒ”ÜûÆ°€‘=ú¾ëùÛ™# ©ì
w–šOú;ÿL˜u„Áá]’–0¯÷u—bŠOĞÏÅ¶¨İ¿µ¡Ñ;Sü ü¾|#K'ÏÊ˜iwäÿ•Úûaä­Åú6Ÿƒ¨Ûü¡%Ú6÷©²Í}BU×wÅª6ÏÜJ¡®kF¾Iµ„©IJ„Õ_m'E½ş«†Ò½ÿ‰éù'¾{zOÖ¥óÒ[®ÁíğÓKjĞ|ñT¤';ÕA%(;¼‚eR’¤ÌSºÒ”3ìÕHÔìÔlìK®Ìm1ºÖ) Gƒˆ½+—3w6: èy«-çpPĞî˜Äq€ß—Ñ®Jêâ€-õÇÓ¾¸jZî^Hö Åq¬|§4¾4æïş’,ü}ø*ûóCAo5_ÍÔeÍ×¤®Æ…QªñT&ªi•…ªÄ•FòªÙ$X~c¡¬ÑÍüAŠÁ“‹Ú´Fêb{Q¹Â9Ş¤”E_<&_¹º8²P¦î°òim"·è¤
×<}{²¼NÒí@…9V¸F'3Ù5y¹ÉêL7Ùy‰²SöĞò¿`85©+mK¾h3:sËÆùZ$6ğıwñ~>Ã„'¦©Ë£&ryæ¸ŞÒ]ûÚŞIÌ 5a@şŸu»‡åÙ«Ú:‘ÕvĞ­ÀÊmd„ûpSØ+È·]MW€ùbòüšo|§û¦€ÓÍª›”±ÉÆÿÕ¡›
W„8p !Â¹©KLİíÔ8æF³pÅ‹†´fø:fvMVƒ>/Z{Êpán‚íœ“?ÉJx--º/ÉŠÂ¹7À_Ÿ.zÁ:ÇÿŸã0Â!åßÅíÄr­Pì.ïŠ}vğ;øÌ»‡Ï¾|¿wñmwDgöOjEôèKôÂd7õ™‡^CoFF/:Òı¯gx¶0vÖÖÈ‡îßBø¹óĞy?¼±} ÔBŞ-SQï„-AÉş,¾õ7¹û7³Õü~ÍqYs¹±©óÇuéSˆw«„¾ùş~:s8Ù7¿^BgÌm•ÌÎö~q¿]r*v}½dÎ™U:¿^¿Y|·-X:ç>Šs²ïqğ ‘„UÍş  ÿÿ¢Õú2¨3Î'İpaij1xŸ!(.Ñãy¬"‰çĞ"²:îN–®®fæXêÜScàmY@WUb›:ØxNô
ö÷ƒş9Y×„WANb	èüQ¬+•Ğ½Hßy)tÛ©05 )”ÀõQÈ±DÑ¼š*3X*¡h²ûÈ›ÈÁ’EaÉ¬n(wzæ•MåQÜ=7´XGºVC÷€\L…î
ºŞQ…´¿®
İJÿlR{ØM!t¥º2»1xŒ¨öğ@¹f#I¯¥LLLÍÌŒÍ1ëØä,êbZ7K ¶ÙØŸÔp!vQáÀÖPQë\<	•ÚIÃÌĞÙÑÈÒKcÜLIÊ/Á¹³g±™r*ø¨iìgÍÑ4¹AOÊ–R˜—HÑó‹™@Á@ê•íŞ‡ø†ä6Tpnv¶\QË   ÿÿ Öøö   ÿÿ ê&\º