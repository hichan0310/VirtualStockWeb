const mysql = require('mysql');
const dbconfig = require('./config/database.js');
const connection = mysql.createConnection(dbconfig);

function nowprice_update() {
    connection.query('SELECT * from nowprice', (error, rows) => { // nowprice가 table 이름임
        if (error) throw error;
        console.log(rows['0']['price']); //rows[행 번호][열 이름]
        return rows['0']['price'];
    });
}
