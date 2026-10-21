package chunkcomfort.api;

/** Lets comfort top up a running potion effect's duration in place (see PotionEffectMixin). */
public interface IRefreshableEffect {

    void chunkcomfort$setDuration(int duration);

}
