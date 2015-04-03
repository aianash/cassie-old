namespace java com.goshoplane.common
namespace js goshoplane.common

typedef string Url
typedef string City
typedef string Country
typedef string Pincode
typedef string AddressTitle
typedef string AddressShort
typedef string AddressFull
typedef double Latitude
typedef double Longitude


enum UserIdType {
  FB = 1;
  Higgs = 2;
}

struct UserId {
  1: i64 uuid;
  2: optional UserIdType type;
}

struct UserName {
  1: optional string first;
  2: optional string last;
  3: optional string handle;
}

struct UserAvatar {
  1: optional Url small;
  2: optional Url medium;
  3: optional Url large;
}

enum StoreType {
  CLOTHING = 1;
  ELECTRONICS = 2;
}

struct StoreId {
  1: i64 stuid;
  2: StoreType type;
}

enum ItemType {
  apparel_men = 1;
  apparel_women = 2;
}

struct CatalogueItemId {
  1: i64 cuid;
  2: StoreId storeId;
}

enum SerializerType {
  MSGPCK = 1;
  JSON   = 2;
  Kryo   = 3;
}

struct SerializerId {
  1: string sid;
  2: SerializerType type;
}

struct SerializedCatalogueItem {
  1: CatalogueItemId itemId;
  2: SerializerId serializerId;
  3: binary stream;
}

struct GPSLocation {
  1: Latitude lat;
  2: Longitude lng;
}

struct PostalAddress {
  1: optional GPSLocation gpsLoc;
  2: optional AddressTitle title;
  3: optional AddressShort short;
  4: optional AddressFull full;
  5: optional Pincode pincode;
  6: optional Country country;
  7: optional City city;
}