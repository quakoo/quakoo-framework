package com.quakoo.baseFramework.image;



public class Exif {
	private String model;
	private String make;
	private String dateTime;
	private Integer imageLength;
	private Integer imageWidth;
	private Integer orientation;
	private Integer whiteBalance;
	private String focalLength;
	private Integer flash;
	private String exposureTime;
	private String isoSpeedRatings;
	private String fNumber;
	private String gpsProcessingMethod;
	private String gpsDateStamp;
	private String gpsTimeStamp;
	private String gpsLongitude;
	private String gpsLongitudeRef;
	private String gpsLatitude;
	private String gpsLatitudeRef;
	private String gpsAltitude;
	private Integer gpsAltitudeRef;

	public Exif() {
		super();
	}

	public Exif(Integer imageLength, Integer imageWidth) {
		super();
		this.imageLength = imageLength;
		this.imageWidth = imageWidth;
	}

	public Exif(Exif exif) {

		this.dateTime = exif.getDateTime();
		this.exposureTime = exif.getExposureTime();
		this.flash = exif.getFlash();
		this.fNumber = exif.getfNumber();
		this.focalLength = exif.getFocalLength();
		this.gpsAltitude = exif.getGpsAltitude();
		this.gpsAltitudeRef = exif.getGpsAltitudeRef();
		this.gpsLatitude = exif.getGpsLatitude();
		this.gpsLatitudeRef = exif.getGpsLatitudeRef();
		this.gpsLongitude = exif.getGpsLongitude();
		this.gpsLongitudeRef = exif.getGpsLongitudeRef();
		this.gpsProcessingMethod = exif.getGpsProcessingMethod();
		this.gpsDateStamp = exif.getGpsDateStamp();
		this.gpsTimeStamp = exif.getGpsTimeStamp();
		this.imageLength = exif.getImageLength();
		this.imageWidth = exif.getImageWidth();
		this.isoSpeedRatings = exif.getIsoSpeedRatings();
		this.make = exif.getMake();
		this.model = exif.getModel();
		this.orientation = exif.getOrientation();
		this.whiteBalance = exif.getWhiteBalance();

	}

	public Exif checkNULL() {
		if (model == null && make == null && dateTime == null && imageLength == null && imageWidth == null && orientation == null
				&& whiteBalance == null && focalLength == null && flash == null && exposureTime == null && isoSpeedRatings == null && fNumber == null
				&& gpsProcessingMethod == null && gpsDateStamp == null && gpsTimeStamp == null && gpsLongitude == null && gpsLongitudeRef == null
				&& gpsLatitude == null && gpsLatitudeRef == null && gpsAltitude == null && gpsAltitudeRef == null) {
			return null;
		} else {
			return this;
		}

	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public String getMake() {
		return make;
	}

	public void setMake(String make) {
		this.make = make;
	}

	public String getDateTime() {
		return dateTime;
	}

	public void setDateTime(String dateTime) {
		this.dateTime = dateTime;
	}

	public Integer getImageLength() {
		return imageLength;
	}

	public void setImageLength(Integer imageLength) {
		this.imageLength = imageLength;
	}

	public Integer getImageWidth() {
		return imageWidth;
	}

	public void setImageWidth(Integer imageWidth) {
		this.imageWidth = imageWidth;
	}

	public Integer getOrientation() {
		return orientation;
	}

	public void setOrientation(Integer orientation) {
		this.orientation = orientation;
	}

	public Integer getWhiteBalance() {
		return whiteBalance;
	}

	public void setWhiteBalance(Integer whiteBalance) {
		this.whiteBalance = whiteBalance;
	}

	public String getFocalLength() {
		return focalLength;
	}

	public void setFocalLength(String focalLength) {
		this.focalLength = focalLength;
	}

	public Integer getFlash() {
		return flash;
	}

	public void setFlash(Integer flash) {
		this.flash = flash;
	}

	public String getExposureTime() {
		return exposureTime;
	}

	public void setExposureTime(String exposureTime) {
		this.exposureTime = exposureTime;
	}

	public String getIsoSpeedRatings() {
		return isoSpeedRatings;
	}

	public void setIsoSpeedRatings(String isoSpeedRatings) {
		this.isoSpeedRatings = isoSpeedRatings;
	}

	public String getfNumber() {
		return fNumber;
	}

	public void setfNumber(String fNumber) {
		this.fNumber = fNumber;
	}

	public String getGpsProcessingMethod() {
		return gpsProcessingMethod;
	}

	public void setGpsProcessingMethod(String gpsProcessingMethod) {
		this.gpsProcessingMethod = gpsProcessingMethod;
	}

	public String getGpsDateStamp() {
		return gpsDateStamp;
	}

	public void setGpsDateStamp(String gpsDateStamp) {
		this.gpsDateStamp = gpsDateStamp;
	}

	public String getGpsTimeStamp() {
		return gpsTimeStamp;
	}

	public void setGpsTimeStamp(String gpsTimeStamp) {
		this.gpsTimeStamp = gpsTimeStamp;
	}

	public String getGpsLongitude() {
		return gpsLongitude;
	}

	public void setGpsLongitude(String gpsLongitude) {
		this.gpsLongitude = gpsLongitude;
	}

	public String getGpsLongitudeRef() {
		return gpsLongitudeRef;
	}

	public void setGpsLongitudeRef(String gpsLongitudeRef) {
		this.gpsLongitudeRef = gpsLongitudeRef;
	}

	public String getGpsLatitude() {
		return gpsLatitude;
	}

	public void setGpsLatitude(String gpsLatitude) {
		this.gpsLatitude = gpsLatitude;
	}

	public String getGpsLatitudeRef() {
		return gpsLatitudeRef;
	}

	public void setGpsLatitudeRef(String gpsLatitudeRef) {
		this.gpsLatitudeRef = gpsLatitudeRef;
	}

	public String getGpsAltitude() {
		return gpsAltitude;
	}

	public void setGpsAltitude(String gpsAltitude) {
		this.gpsAltitude = gpsAltitude;
	}

	public Integer getGpsAltitudeRef() {
		return gpsAltitudeRef;
	}

	public void setGpsAltitudeRef(Integer gpsAltitudeRef) {
		this.gpsAltitudeRef = gpsAltitudeRef;
	}

	@Override
	public String toString() {
		return "Exif [" + (model != null ? "model=" + model + ", " : "") + (make != null ? "make=" + make + ", " : "")
				+ (dateTime != null ? "dateTime=" + dateTime + ", " : "") + (imageLength != null ? "imageLength=" + imageLength + ", " : "")
				+ (imageWidth != null ? "imageWidth=" + imageWidth + ", " : "") + (orientation != null ? "orientation=" + orientation + ", " : "")
				+ (whiteBalance != null ? "whiteBalance=" + whiteBalance + ", " : "")
				+ (focalLength != null ? "focalLength=" + focalLength + ", " : "") + (flash != null ? "flash=" + flash + ", " : "")
				+ (exposureTime != null ? "exposureTime=" + exposureTime + ", " : "")
				+ (isoSpeedRatings != null ? "isoSpeedRatings=" + isoSpeedRatings + ", " : "") + (fNumber != null ? "fNumber=" + fNumber + ", " : "")
				+ (gpsProcessingMethod != null ? "gpsProcessingMethod=" + gpsProcessingMethod + ", " : "")
				+ (gpsDateStamp != null ? "gpsDateStamp=" + gpsDateStamp + ", " : "")
				+ (gpsTimeStamp != null ? "gpsTimeStamp=" + gpsTimeStamp + ", " : "")
				+ (gpsLongitude != null ? "gpsLongitude=" + gpsLongitude + ", " : "")
				+ (gpsLongitudeRef != null ? "gpsLongitudeRef=" + gpsLongitudeRef + ", " : "")
				+ (gpsLatitude != null ? "gpsLatitude=" + gpsLatitude + ", " : "")
				+ (gpsLatitudeRef != null ? "gpsLatitudeRef=" + gpsLatitudeRef + ", " : "")
				+ (gpsAltitude != null ? "gpsAltitude=" + gpsAltitude + ", " : "")
				+ (gpsAltitudeRef != null ? "gpsAltitudeRef=" + gpsAltitudeRef : "") + "]";
	}

}
