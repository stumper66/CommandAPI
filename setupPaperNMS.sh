echo "Setup Paper NMS for version 1.18..."
mvn --quiet paper-nms:init -pl :commandapi-paper-1.18 -P Platform.Paper

echo "Setup Paper NMS for version 1.18.2..."
mvn --quiet paper-nms:init -pl :commandapi-paper-1.18.2 -P Platform.Paper

echo "Setup Paper NMS for version 1.19..."
mvn --quiet paper-nms:init -pl :commandapi-paper-1.19 -P Platform.Paper

echo "Setup Paper NMS for version 1.19.1..."
mvn --quiet paper-nms:init -pl :commandapi-paper-1.19.1 -P Platform.Paper

echo "Setup Paper NMS for version 1.19.3..."
mvn --quiet paper-nms:init -pl :commandapi-paper-1.19.3 -P Platform.Paper

echo "Setup Paper NMS for version 1.19.4..."
mvn --quiet paper-nms:init -pl :commandapi-paper-1.19.4 -P Platform.Paper

echo "Setup Paper NMS for version 1.20..."
mvn --quiet paper-nms:init -pl :commandapi-paper-1.20 -P Platform.Paper

echo "Setup Paper NMS for version 1.20.2..."
mvn --quiet paper-nms:init -pl :commandapi-paper-1.20.2 -P Platform.Paper

echo "Setup Paper NMS for version 1.20.3..."
mvn --quiet paper-nms:init -pl :commandapi-paper-1.20.3 -P Platform.Paper

echo "Setup Paper NMS for version 1.20.5..."
mvn --quiet paper-nms:init -pl :commandapi-paper-1.20.5 -P Platform.Paper

echo "Setup Paper NMS for version 1.21..."
mvn --quiet paper-nms:init -pl :commandapi-paper-1.21 -P Platform.Paper

echo "Done!"