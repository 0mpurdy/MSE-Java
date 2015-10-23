package mse.data;

/**
 * Created by mj_pu_000 on 23/10/2015.
 */
public class Map {

    String area;
    String mapName;
    String mapLocation;

    public Map(String area, String mapName, String mapLocation) {
        this.area = area;
        this.mapName = mapName;
        this.mapLocation = mapLocation;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getMapName() {
        return mapName;
    }

    public void setMapName(String mapName) {
        this.mapName = mapName;
    }

    public String getMapLocation() {
        return mapLocation;
    }

    public void setMapLocation(String mapLocation) {
        this.mapLocation = mapLocation;
    }
}
