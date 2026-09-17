package me.imshy.pictogram.social.internal;

import me.imshy.pictogram.social.SocialStats;
import org.springframework.stereotype.Service;

@Service
class SocialStatsService implements SocialStats {

    private final FollowGraph followGraph;
    private final LikeVolume likeVolume;
    private final CommentVolume commentVolume;

    SocialStatsService(FollowGraph followGraph, LikeVolume likeVolume, CommentVolume commentVolume) {
        this.followGraph = followGraph;
        this.likeVolume = likeVolume;
        this.commentVolume = commentVolume;
    }

    @Override
    public long totalFollows() {
        return followGraph.totalFollows();
    }

    @Override
    public long totalLikes() {
        return likeVolume.total();
    }

    @Override
    public long totalComments() {
        return commentVolume.total();
    }
}
