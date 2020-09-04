echo Making jar
./gradlew jar
echo Copying
cp ./build/libs/CampaignPlugin.jar ~/Documents/mindustry/game/server/campaign-server/config/mods

