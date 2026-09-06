package me.imshy.chat.ws;

import me.imshy.chat.UserId;

record SendMessageRequest(UserId recipientUserId, String text) {
}
