
// Mock data for Buy-02
const client_id = "mock_client_id";
const seller_id = "mock_seller_id";
const password_hash = "$2a$10$tOWtSaDTO1Ph.lJ1odao8OuOsOJt31kj.2k8Ln8h0kvj1oR1OyxyW"; // password123 confirmed hash

// 1. Setup Users
db = db.getSiblingDB('userdb');
db.users.drop();
db.users.insertMany([
    {
        _id: client_id,
        name: "John Client",
        email: "client@test.com",
        password: password_hash,
        role: "CLIENT",
        _class: "ax.gritlab.buy_01.user.model.User"
    },
    {
        _id: seller_id,
        name: "Jane Seller",
        email: "seller@test.com",
        password: password_hash,
        role: "SELLER",
        _class: "ax.gritlab.buy_01.user.model.User"
    }
]);

const media_url_prefix = "http://localhost:8090/api/media/images/";

// 2. Setup Products
db = db.getSiblingDB('productdb');
db.products.drop();
db.products.insertMany([
    {
        _id: "prod_1",
        name: "Nokia 3310",
        description: "Unbreakable phone. Classic blue color.",
        price: 19.99,
        quantity: 10,
        userId: seller_id,
        category: "Electronics",
        mediaIds: ["63e3aea6-318c-414e-a87e-8482febbaa61"],
        imageUrls: [media_url_prefix + "63e3aea6-318c-414e-a87e-8482febbaa61"],
        _class: "ax.gritlab.buy_01.product.model.Product",
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        _id: "prod_2",
        name: "Bazooka bubble gum 6 pack",
        description: "Not sure if safe to eat.",
        price: 0.99,
        quantity: 20,
        userId: seller_id,
        category: "Food",
        mediaIds: ["e7915468-ff09-406b-b843-529a0a4849bb"],
        imageUrls: [media_url_prefix + "e7915468-ff09-406b-b843-529a0a4849bb"],
        _class: "ax.gritlab.buy_01.product.model.Product",
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        _id: "prod_3",
        name: "Samsung 3.5 inch floppy disk",
        description: "Save your data on a disk. Safe, durable, ultrathin, and reliable. Nobody is going to steal it!",
        price: 2.59,
        quantity: 5,
        userId: seller_id,
        category: "Electronics",
        mediaIds: ["796e3151-0fb0-48e6-bcc0-b6f1ce9608c7"],
        imageUrls: [media_url_prefix + "796e3151-0fb0-48e6-bcc0-b6f1ce9608c7"],
        _class: "ax.gritlab.buy_01.product.model.Product",
        createdAt: new Date(),
        updatedAt: new Date()
    }
]);

// 2.5 Setup Media Metadata
db = db.getSiblingDB('mediadb');
db.media.drop();
db.media.insertMany([
    {
        _id: "63e3aea6-318c-414e-a87e-8482febbaa61",
        originalFilename: "nokia.jpg",
        contentType: "image/jpeg",
        size: 552143,
        filePath: "/Users/jedi.reston/buy-02/media-service/uploads/63e3aea6-318c-414e-a87e-8482febbaa61.jpg",
        userId: seller_id,
        productId: "prod_1",
        url: media_url_prefix + "63e3aea6-318c-414e-a87e-8482febbaa61",
        createdAt: new Date(),
        updatedAt: new Date(),
        _class: "ax.gritlab.buy_01.media.model.Media"
    },
    {
        _id: "e7915468-ff09-406b-b843-529a0a4849bb",
        originalFilename: "bazooka.jpg",
        contentType: "image/jpeg",
        size: 18486,
        filePath: "/Users/jedi.reston/buy-02/media-service/uploads/e7915468-ff09-406b-b843-529a0a4849bb.jpg",
        userId: seller_id,
        productId: "prod_2",
        url: media_url_prefix + "e7915468-ff09-406b-b843-529a0a4849bb",
        createdAt: new Date(),
        updatedAt: new Date(),
        _class: "ax.gritlab.buy_01.media.model.Media"
    },
    {
        _id: "796e3151-0fb0-48e6-bcc0-b6f1ce9608c7",
        originalFilename: "samsung.jpg",
        contentType: "image/jpeg",
        size: 450424,
        filePath: "/Users/jedi.reston/buy-02/media-service/uploads/796e3151-0fb0-48e6-bcc0-b6f1ce9608c7.jpg",
        userId: seller_id,
        productId: "prod_3",
        url: media_url_prefix + "796e3151-0fb0-48e6-bcc0-b6f1ce9608c7",
        createdAt: new Date(),
        updatedAt: new Date(),
        _class: "ax.gritlab.buy_01.media.model.Media"
    }
]);

// 3. Setup Orders
db = db.getSiblingDB('orderdb');
db.orders.drop();
db.orders.insertMany([
    {
        _id: "ord_1",
        orderNumber: "ORD-2024-001",
        buyerId: client_id,
        buyerName: "John Client",
        buyerEmail: "client@test.com",
        items: [
            {
                productId: "prod_1",
                productName: "Nokia 3310",
                quantity: 1,
                priceAtPurchase: 19.99,
                subtotal: 19.99,
                sellerId: seller_id,
                category: "Electronics",
                _class: "ax.gritlab.buy_01.order.model.OrderItem"
            }
        ],
        sellerIds: [seller_id],
        subtotal: 19.99,
        totalAmount: 19.99,
        status: "DELIVERED",
        createdAt: new Date(),
        updatedAt: new Date(),
        _class: "ax.gritlab.buy_01.order.model.Order"
    },
    {
        _id: "ord_2",
        orderNumber: "ORD-2024-002",
        buyerId: client_id,
        buyerName: "John Client",
        buyerEmail: "client@test.com",
        items: [
            {
                productId: "prod_2",
                productName: "Bazooka bubble gum 6 pack",
                quantity: 2,
                priceAtPurchase: 0.99,
                subtotal: 1.98,
                sellerId: seller_id,
                category: "Food",
                _class: "ax.gritlab.buy_01.order.model.OrderItem"
            }
        ],
        sellerIds: [seller_id],
        subtotal: 1.98,
        totalAmount: 1.98,
        status: "DELIVERED",
        createdAt: new Date(),
        updatedAt: new Date(),
        _class: "ax.gritlab.buy_01.order.model.Order"
    },
    {
        _id: "ord_3",
        orderNumber: "ORD-2024-003",
        buyerId: client_id,
        buyerName: "John Client",
        buyerEmail: "client@test.com",
        items: [
            {
                productId: "prod_3",
                productName: "Samsung 3.5 inch floppy disk",
                quantity: 1,
                priceAtPurchase: 2.59,
                subtotal: 2.59,
                sellerId: seller_id,
                category: "Electronics",
                _class: "ax.gritlab.buy_01.order.model.OrderItem"
            }
        ],
        sellerIds: [seller_id],
        subtotal: 2.59,
        totalAmount: 2.59,
        status: "DELIVERED",
        createdAt: new Date(),
        updatedAt: new Date(),
        _class: "ax.gritlab.buy_01.order.model.Order"
    }
]);
