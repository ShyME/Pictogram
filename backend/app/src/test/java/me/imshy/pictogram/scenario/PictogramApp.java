package me.imshy.pictogram.scenario;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface PictogramApp {

    PictogramApi registerViaGoogle(String email);

    interface PictogramApi {

        String accessToken();

        Optional<Profile> currentProfile();

        Profile completeOnboarding(String username);

        Profile completeOnboarding(String username, String displayName, String bio);

        Profile editProfile(String username, String displayName, String bio);

        Optional<Profile> viewProfile(String username);

        String uploadPhoto(byte[] image);

        Post publishPost(String mediaId, String caption);

        DeleteOutcome deletePost(String postId);

        List<Post> postsOf(String userId);

        FollowOutcome follow(String userId);

        void unfollow(String userId);

        FollowRelationship followRelationship(String userId);

        Map<String, FollowRelationship> followRelationships(String... userIds);

        void like(String postId);

        void unlike(String postId);

        Map<String, PostLikes> likesOf(String... postIds);

        Comment comment(String postId, String body);

        CommentPage commentsOn(String postId, String cursor, Integer limit);

        DeleteOutcome deleteComment(String commentId);

        Map<String, Long> commentCountsOf(String... postIds);

        AccountPage followers(String userId, String cursor, Integer limit);

        AccountPage following(String userId, String cursor, Integer limit);

        FeedPage openFeed();

        FeedPage openFeed(String cursor, Integer limit);

        NotificationPage openNotifications();

        NotificationPage openNotifications(String cursor, Integer limit);

        long unreadNotificationCount();

        void markNotificationsRead();
    }

    enum DeleteOutcome {
        DELETED, FORBIDDEN
    }

    enum FollowOutcome {
        OK, SELF_FOLLOW
    }

    record FollowRelationship(long followerCount, long followingCount, boolean followedByViewer) {
    }

    record PostLikes(long likeCount, boolean likedByViewer) {
    }

    record Comment(String commentId, String postId, String authorId, String body, String createdAt) {
    }

    record CommentPage(List<Comment> comments, String nextCursor) {
    }

    record AccountPage(List<String> userIds, String nextCursor) {
    }

    record Notification(String type, String actorId, String subjectPostId, String occurredAt, boolean read) {
    }

    record NotificationPage(List<Notification> notifications, String nextCursor) {
    }

    record Profile(String userId, String username, String displayName, String bio) {
    }

    record Post(String postId, String authorId, String mediaId, String caption, String publishedAt) {
    }

    record FeedPage(List<Post> posts, String nextCursor) {

        public boolean isEmpty() {
            return posts.isEmpty() && nextCursor == null;
        }

        public List<String> postIds() {
            return posts.stream().map(Post::postId).toList();
        }
    }
}
