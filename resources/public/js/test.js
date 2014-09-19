var createResource = function(path, data, fn) {
    $.ajax({
        type: "POST",
        url: path,
        async: false,
        data: data,
        dataType: "json",
        success: fn
    });
};

describe("Mea webservice API", function() {
    it("should create a participant", function() {
        var ppts = [
              {"first_name": "Peter", "last_name": "Parker"}
            , {"first_name": "Bruce", "last_name": "Banner"}
            , {"first_name": "Tony", "last_name": "Stark"}
        ];
        _.each(ppts, function(ppt){
            createResource('/participants', ppt, function(data) {
                expect(data.name).toBe(ppt.first_name + " " + ppt.last_name)
            });
        });
    });

    it("should create a study", function() {
        var studies = [
              {"keyword": "grade", "human_name": "GRADE"}
            , {"keyword": "confirm", "human_name": "CONFIRM"}
        ];
        _.each(studies, function(study){
            createResource('/studies', study, function(data) {
                console.log(data);
                expect(data.keyword).toBe(study.keyword);
                expect(data.human_name).toBe(study.human_name);
            });
        });
    });
});
